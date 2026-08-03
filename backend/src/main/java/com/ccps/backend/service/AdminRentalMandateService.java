package com.ccps.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminRentalMandateCreateRequest;
import com.ccps.backend.dto.AdminRentalMandateResponse;
import com.ccps.backend.dto.AdminRentalMandateResponse.History;
import com.ccps.backend.dto.AdminRentalMandateResponse.Item;
import com.ccps.backend.dto.AdminRentalMandateResponse.Options;
import com.ccps.backend.dto.AdminRentalMandateResponse.Page;
import com.ccps.backend.dto.AdminRentalMandateResponse.Summary;
import com.ccps.backend.dto.AdminRentalMandateResponse.UserOption;
import com.ccps.backend.dto.AdminRentalMandateReviewRequest;
import com.ccps.backend.dto.AdminRentalMandateStatusRequest;
import com.ccps.backend.mapper.AdminRentalMandateMapper;
import com.ccps.backend.mapper.AdminRentalMandateMapper.HistoryRow;
import com.ccps.backend.mapper.AdminRentalMandateMapper.MandateRow;
import com.ccps.backend.mapper.AdminRentalMandateMapper.NewMandate;
import com.ccps.backend.mapper.AdminRentalMandateMapper.ExpiredMandateRow;

@Service
public class AdminRentalMandateService {
    private static final List<String> MANDATE_TYPES = List.of("management", "exclusive", "non_exclusive");
    private static final List<String> ACTIVE_STATUSES = List.of("active", "suspended");
    private final AdminRentalMandateMapper mapper;

    public AdminRentalMandateService(AdminRentalMandateMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public AdminRentalMandateResponse find(int page, int pageSize, String keyword, String status) {
        expireEndedMandates();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, pageSize));
        String normalizedStatus = normalize(status);
        long total = mapper.countPage(normalizedStatus, normalize(keyword));
        List<Item> rows = mapper.findPage(normalizedStatus, normalize(keyword), safeSize, (safePage - 1) * safeSize)
                .stream().map(this::item).toList();
        return new AdminRentalMandateResponse(rows, summary(), new Page(total, safePage, safeSize,
                total == 0 ? 1 : (int) Math.ceil((double) total / safeSize)));
    }

    @Transactional
    public Options options() {
        expireEndedMandates();
        return new Options(
                mapper.findOperatingUnits().stream()
                        .map(row -> new AdminRentalMandateResponse.Option(row.getId(), row.getLabel(), row.getOwnerId(),
                                row.getOwnerName(), row.getProjectId(), row.getProjectName(), row.getUnitNo())).toList(),
                mapper.findActiveUsers().stream().map(row -> new UserOption(row.getId(), row.getName())).toList());
    }

    @Transactional(readOnly = true)
    public List<History> history(Long mandateId) {
        requireMandate(mandateId);
        return mapper.findHistory(mandateId).stream().map(this::history).toList();
    }

    @Transactional
    public Item create(Long actorId, AdminRentalMandateCreateRequest request) {
        expireEndedMandates();
        if (!MANDATE_TYPES.contains(request.mandateType())) {
            throw bad("Unsupported rental mandate type");
        }
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw bad("Mandate end date cannot be before start date");
        }
        if (mapper.countOperatingUnit(request.ownerUnitId()) != 1) {
            throw bad("Rental mandate requires an active operating property");
        }
        if (mapper.countOverlapping(request.ownerUnitId(), request.startDate(), request.endDate()) > 0) {
            throw conflict("An overlapping rental mandate already exists for this property");
        }
        if (request.responsibleUserId() != null && mapper.countActiveUser(request.responsibleUserId()) != 1) {
            throw bad("Responsible user is not active");
        }
        NewMandate mandate = new NewMandate();
        mandate.setOwnerUnitId(request.ownerUnitId());
        mandate.setMandateNo("RM-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT));
        mandate.setMandateType(request.mandateType());
        mandate.setStartDate(request.startDate()); mandate.setEndDate(request.endDate());
        mandate.setManagementFee(request.managementFee()); mandate.setCommissionPercent(request.commissionPercent());
        mandate.setResponsibleUserId(request.responsibleUserId()); mandate.setCreatedBy(actorId);
        if (mapper.insertMandate(mandate) != 1 || mandate.getId() == null) {
            throw conflict("Unable to create rental mandate");
        }
        mapper.insertHistory(mandate.getId(), null, "draft", "建立租管委託", actorId);
        audit(actorId, "create", mandate.getId(), null, "{\"status\":\"draft\"}");
        return item(mapper.findById(mandate.getId()));
    }

    @Transactional
    public int expireEndedMandates() {
        int expired = 0;
        for (ExpiredMandateRow candidate : mapper.findExpiredMandates()) {
            if (mapper.expireMandate(candidate.getId(), candidate.getStatus()) != 1) continue;
            mapper.insertHistory(candidate.getId(), candidate.getStatus(), "expired", "委托期限已到期", null);
            audit(null, "auto_expire", candidate.getId(),
                    "{\"status\":\"" + candidate.getStatus() + "\"}",
                    "{\"status\":\"expired\",\"reason\":\"委托期限已到期\"}");
            mapper.updateRentalService(candidate.getId(), "terminated");
            expired++;
        }
        return expired;
    }

    @Transactional
    public Item submit(Long actorId, Long mandateId) {
        String current = lock(mandateId);
        if (!"draft".equals(current)) throw conflict("Only draft mandates can be submitted");
        transition(actorId, mandateId, current, "pending_review", "提交審核", null, null, null);
        return item(mapper.findById(mandateId));
    }

    @Transactional
    public Item review(Long actorId, Long mandateId, AdminRentalMandateReviewRequest request) {
        String current = lock(mandateId);
        if (!List.of("draft", "pending_review").contains(current)) {
            throw conflict("Only draft or pending mandates can be reviewed");
        }
        if (Boolean.TRUE.equals(request.approved()) && mapper.countAuthorizationDocuments(mandateId) == 0) {
            throw conflict("请先上传业主签署的授权委托书，再启用出租委托");
        }
        String next = Boolean.TRUE.equals(request.approved()) ? "active" : "draft";
        transition(actorId, mandateId, current, next, request.note(), actorId,
                request.note(), null);
        if ("active".equals(next)) mapper.activateRentalService(mandateId);
        return item(mapper.findById(mandateId));
    }

    @Transactional
    public Item updateStatus(Long actorId, Long mandateId, AdminRentalMandateStatusRequest request) {
        String current = lock(mandateId);
        String next = request.status();
        if ("active".equals(next) && !List.of("suspended").contains(current)) {
            throw conflict("Only suspended mandates can be reactivated");
        }
        if ("suspended".equals(next) && !"active".equals(current)) {
            throw conflict("Only active mandates can be suspended");
        }
        if ("terminated".equals(next) && !ACTIVE_STATUSES.contains(current)) {
            throw conflict("Only active or suspended mandates can be terminated");
        }
        if ("terminated".equals(next) && (request.reason() == null || request.reason().isBlank())) {
            throw bad("Termination reason is required");
        }
        if ("terminated".equals(next) && ACTIVE_STATUSES.contains(current)
                && mapper.countActiveLeasesForMandate(mandateId) > 0) {
            throw conflict("該房產仍有有效租約，請先結束或轉移租約後再終止出租委託");
        }
        transition(actorId, mandateId, current, next, request.reason(), actorId, null,
                "terminated".equals(next) ? request.reason() : null);
        mapper.updateRentalService(mandateId, next);
        if ("active".equals(next)) mapper.activateRentalService(mandateId);
        return item(mapper.findById(mandateId));
    }

    private void transition(Long actorId, Long mandateId, String from, String to, String reason,
            Long reviewedBy, String reviewNote, String terminationReason) {
        int updated = mapper.updateStatus(mandateId, to, reviewedBy,
                reviewedBy == null ? null : LocalDateTime.now(), reviewNote, terminationReason);
        if (updated != 1) throw conflict("Rental mandate was changed by another request");
        mapper.insertHistory(mandateId, from, to, reason, actorId);
        audit(actorId, "status_change", mandateId, "{\"status\":\"" + from + "\"}",
                "{\"status\":\"" + to + "\"}");
    }

    private Summary summary() {
        var row = mapper.summary();
        return new Summary(value(row.getTotal()), value(row.getDraft()), value(row.getPendingReview()),
                value(row.getActive()), value(row.getSuspended()), value(row.getTerminated()));
    }

    private Item item(MandateRow row) {
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental mandate not found");
        return new Item(row.getId(), row.getMandateNo(), row.getOwnerUnitId(), row.getOwnerId(), row.getOwnerName(), row.getOwnerIdentity(), row.getOwnerEmail(),
                row.getProjectId(), row.getProjectName(), row.getUnitNo(), row.getMandateType(), row.getStartDate(),
                row.getEndDate(), row.getManagementFee(), row.getCommissionPercent(), row.getResponsibleUserId(),
                row.getResponsibleUserName(), row.getStatus(), row.getReviewNote(), row.getTerminationReason(),
                row.getSubmittedAt(), row.getReviewedAt(), row.getReviewedByName(), row.getCreatedAt());
    }

    private History history(HistoryRow row) {
        return new History(row.getId(), row.getMandateId(), row.getFromStatus(), row.getToStatus(), row.getReason(),
                row.getChangedBy(), row.getChangedByName(), row.getChangedAt());
    }

    private String lock(Long id) {
        requireMandate(id);
        return mapper.lockStatus(id);
    }

    private void requireMandate(Long id) {
        if (id == null || id <= 0 || mapper.findById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental mandate not found");
        }
    }

    private void audit(Long actorId, String action, Long id, String before, String after) {
        mapper.insertAudit(actorId, action, id, before, after);
    }

    private String normalize(String value) { return value == null ? null : value.trim().isEmpty() ? null : value.trim(); }
    private long value(Long value) { return value == null ? 0 : value; }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
