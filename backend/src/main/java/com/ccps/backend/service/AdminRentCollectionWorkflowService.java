package com.ccps.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminRentCollectionWorkflowResponse;
import com.ccps.backend.mapper.AdminRentCollectionWorkflowMapper;
import com.ccps.backend.mapper.AdminRentCollectionWorkflowMapper.CollectionRow;
import com.ccps.backend.mapper.AdminRentCollectionWorkflowMapper.NewNotification;

@Service
public class AdminRentCollectionWorkflowService {
    private static final List<Stage> STAGES = List.of(
            new Stage("first_reminder", "第一封提醒函", 14, "第一次租金逾期提醒", "high"),
            new Stage("second_reminder", "第二封提醒函", 16, "第二次租金逾期提醒", "high"),
            new Stage("final_reminder", "最终提醒函", 18, "最终租金逾期提醒", "urgent"),
            new Stage("termination_notice", "终止通知", 20, "租约终止通知", "urgent"));

    private final AdminRentCollectionWorkflowMapper mapper;
    private final Clock clock;

    @Autowired
    public AdminRentCollectionWorkflowService(AdminRentCollectionWorkflowMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    AdminRentCollectionWorkflowService(AdminRentCollectionWorkflowMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public AdminRentCollectionWorkflowResponse overview() {
        LocalDate today = LocalDate.now(clock);
        List<AdminRentCollectionWorkflowResponse.Item> items = mapper.findOutstanding(today).stream()
                .map(row -> toItem(row, today)).toList();
        long waiting = items.stream().filter(item -> "waiting".equals(item.currentStage())).count();
        long first = countStage(items, "first_reminder");
        long second = countStage(items, "second_reminder");
        long last = countStage(items, "final_reminder");
        long termination = countStage(items, "termination_notice");
        long onHold = items.stream().filter(item -> "on_hold".equals(item.workflowStatus())).count();
        BigDecimal outstanding = items.stream().map(AdminRentCollectionWorkflowResponse.Item::outstandingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return new AdminRentCollectionWorkflowResponse(
                new AdminRentCollectionWorkflowResponse.Summary(items.size(), waiting, first, second, last,
                        termination, onHold, outstanding), items);
    }

    @Transactional
    public AdminRentCollectionWorkflowResponse.Item sendStage(Long actorId, Long invoiceId, String stageCode) {
        Stage requested = requireStage(stageCode);
        CollectionRow row = requireInvoice(invoiceId);
        LocalDate today = LocalDate.now(clock);
        requireOutstandingActive(row);
        if ("on_hold".equals(row.getWorkflowStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This collection case is on hold");
        }
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(row.getDueDate(), today));
        if (overdueDays < requested.thresholdDays()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The selected reminder stage is not due yet");
        }
        Stage available = availableStage(overdueDays, sentStages(row));
        if (available == null || !available.code().equals(requested.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Send the currently due collection stage before continuing");
        }
        if (mapper.countSentStage(invoiceId, requested.code()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This reminder stage was already sent");
        }

        mapper.ensureWorkflow(invoiceId);
        BigDecimal outstanding = outstanding(row);
        String body = messageBody(requested, row, outstanding, overdueDays);
        NewNotification notification = new NewNotification();
        notification.setRecipientUserId(row.getTenantUserId());
        notification.setInvoiceId(invoiceId);
        notification.setTitle(requested.title());
        notification.setBody(body);
        notification.setPriority(requested.priority());
        if (mapper.insertNotification(notification) != 1 || notification.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create the tenant collection notice");
        }
        mapper.insertInAppDelivery(notification.getId());
        if (row.getTenantEmail() != null && !row.getTenantEmail().isBlank()) {
            mapper.insertEmailDelivery(notification.getId(), row.getTenantEmail().trim().toLowerCase());
        }
        if (Boolean.TRUE.equals(row.getWhatsappEnabled())) {
            mapper.insertWhatsAppDelivery(notification.getId(), row.getTenantId());
        }
        if (mapper.insertAction(invoiceId, requested.code(), requested.thresholdDays(),
                row.getDueDate().plusDays(requested.thresholdDays()), requested.title(), body,
                notification.getId(), actorId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The reminder stage was already recorded");
        }
        mapper.insertAudit(actorId, invoiceId, "send_rent_collection_notice", requested.code(),
                requested.code().equals("termination_notice")
                        ? "Termination notice sent; lease closure and access suspension still require manual approval"
                        : requested.label() + " sent to tenant");
        return toItem(mapper.lockInvoice(invoiceId), today);
    }

    @Transactional
    public AdminRentCollectionWorkflowResponse.Item hold(Long actorId, Long invoiceId, String reason) {
        CollectionRow row = requireInvoice(invoiceId);
        requireOutstandingActive(row);
        mapper.ensureWorkflow(invoiceId);
        if (mapper.hold(invoiceId, actorId, reason.trim()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to pause this collection case");
        }
        mapper.insertAudit(actorId, invoiceId, "hold_rent_collection", null, reason.trim());
        return toItem(mapper.lockInvoice(invoiceId), LocalDate.now(clock));
    }

    @Transactional
    public AdminRentCollectionWorkflowResponse.Item resume(Long actorId, Long invoiceId) {
        CollectionRow row = requireInvoice(invoiceId);
        requireOutstandingActive(row);
        mapper.ensureWorkflow(invoiceId);
        if (mapper.resume(invoiceId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to resume this collection case");
        }
        mapper.insertAudit(actorId, invoiceId, "resume_rent_collection", null, "Collection resumed");
        return toItem(mapper.lockInvoice(invoiceId), LocalDate.now(clock));
    }

    private AdminRentCollectionWorkflowResponse.Item toItem(CollectionRow row, LocalDate today) {
        long overdueDays = Math.max(0, ChronoUnit.DAYS.between(row.getDueDate(), today));
        Set<String> sent = sentStages(row);
        Stage available = availableStage(overdueDays, sent);
        Stage next = nextStage(sent);
        boolean hold = "on_hold".equals(row.getWorkflowStatus());
        String currentStage = hold ? "on_hold" : available == null ? "waiting" : available.code();
        String currentLabel = hold ? "暂停催缴" : available == null ? "等待下一阶段" : available.label();
        LocalDate scheduled = available == null ? null : row.getDueDate().plusDays(available.thresholdDays());
        LocalDate nextAction = next == null ? null : row.getDueDate().plusDays(next.thresholdDays());
        return new AdminRentCollectionWorkflowResponse.Item(row.getInvoiceId(), row.getLeaseId(), row.getTenantId(),
                row.getTenantName(), row.getTenantPhone(), row.getTenantEmail(),
                Boolean.TRUE.equals(row.getWhatsappEnabled()), row.getWhatsappDestination(),
                row.getProjectName(), row.getUnitNo(),
                row.getLeaseNo(), row.getBillingMonth(), row.getDueDate(), money(row.getAmountDue()),
                money(row.getAmountPaid()), outstanding(row), overdueDays,
                hold ? "on_hold" : "active", row.getHoldReason(), currentStage, currentLabel,
                scheduled, nextAction, new ArrayList<>(sent), row.getLastSentStage(), row.getLastSentAt());
    }

    private CollectionRow requireInvoice(Long invoiceId) {
        CollectionRow row = mapper.lockInvoice(invoiceId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent invoice not found");
        return row;
    }

    private void requireOutstandingActive(CollectionRow row) {
        if (!"active".equals(row.getLeaseStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active leases can enter rent collection");
        }
        if (outstanding(row).signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent invoice is already paid");
        }
        if (row.getDueDate() == null || !row.getDueDate().isBefore(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent invoice is not overdue");
        }
    }

    private Stage availableStage(long overdueDays, Set<String> sent) {
        return STAGES.stream().filter(stage -> overdueDays >= stage.thresholdDays() && !sent.contains(stage.code()))
                .findFirst().orElse(null);
    }

    private Stage nextStage(Set<String> sent) {
        return STAGES.stream().filter(stage -> !sent.contains(stage.code())).findFirst().orElse(null);
    }

    private Set<String> sentStages(CollectionRow row) {
        if (row.getSentStages() == null || row.getSentStages().isBlank()) return new LinkedHashSet<>();
        return new LinkedHashSet<>(Arrays.asList(row.getSentStages().split(",")));
    }

    private Stage requireStage(String stageCode) {
        return STAGES.stream().filter(stage -> stage.code().equals(stageCode)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown collection stage"));
    }

    private String messageBody(Stage stage, CollectionRow row, BigDecimal outstanding, long overdueDays) {
        String base = "%s租客您好：%s %s 的 %s 租金应于 %s 缴付，目前尚欠 RM %s，已逾期 %d 天。"
                .formatted(row.getTenantName(), row.getProjectName(), row.getUnitNo(),
                        row.getBillingMonth(), row.getDueDate(), moneyText(outstanding), overdueDays);
        if (stage.code().equals("termination_notice")) {
            return base + "此前催缴仍未结清，现发出终止通知。后续解约及门禁停用须由工作人员另行审批办理。";
        }
        return base + "请尽快完成付款；如已付款，请联系管理人员核对。";
    }

    private BigDecimal outstanding(CollectionRow row) {
        return money(row.getAmountDue()).subtract(money(row.getAmountPaid())).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String moneyText(BigDecimal value) { return money(value).toPlainString(); }
    private long countStage(List<AdminRentCollectionWorkflowResponse.Item> items, String stage) {
        return items.stream().filter(item -> stage.equals(item.currentStage())).count();
    }

    private record Stage(String code, String label, int thresholdDays, String title, String priority) {
    }
}
