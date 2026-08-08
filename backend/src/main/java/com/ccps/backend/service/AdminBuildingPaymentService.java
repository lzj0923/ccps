package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminBuildingPaymentResponse;
import com.ccps.backend.dto.AdminInstallmentUpdateRequest;
import com.ccps.backend.dto.AdminPaymentContractOption;
import com.ccps.backend.dto.AdminPaymentPlanCreateRequest;
import com.ccps.backend.dto.AdminPaymentPlanCreateResponse;
import com.ccps.backend.dto.AdminProjectCreateRequest;
import com.ccps.backend.dto.AdminProjectOption;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.InstallmentRow;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.InstallmentActionContext;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.NewPaymentInstallment;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.NewPaymentPlan;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.NewProject;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.ReminderContext;

@Service
public class AdminBuildingPaymentService {
    private final AdminBuildingPaymentMapper mapper;

    public AdminBuildingPaymentService(AdminBuildingPaymentMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public AdminProjectOption createProject(AdminProjectCreateRequest request) {
        String projectCode = request.projectCode().trim().toUpperCase();
        if (mapper.countProjectCode(projectCode) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project code already exists");
        }
        NewProject project = new NewProject();
        project.setProjectCode(projectCode);
        project.setName(request.name().trim());
        project.setAddress(normalize(request.address()));
        project.setState(normalize(request.state()));
        project.setCity(normalize(request.city()));
        project.setCountryCode(request.countryCode().trim().toUpperCase());
        project.setStatus(request.status());
        if (mapper.insertProject(project) != 1 || project.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Project could not be created");
        }
        return new AdminProjectOption(project.getId(), project.getProjectCode(), project.getName(), project.getCity());
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentContractOption> findPaymentContracts() {
        return mapper.findPaymentContracts();
    }

    @Transactional
    public AdminPaymentPlanCreateResponse createPaymentPlan(AdminPaymentPlanCreateRequest request) {
        if (mapper.lockPurchaseContract(request.purchaseContractId()) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active purchase contract not found");
        }
        AdminPaymentContractOption contract = mapper.findPaymentContract(request.purchaseContractId());
        if (contract == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Contract is not eligible for a payment plan");
        }
        if (mapper.countActivePaymentPlans(contract.contractId()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An active payment plan already exists for this contract");
        }

        BigDecimal total = BigDecimal.ZERO;
        LocalDate previousDueDate = null;
        for (int index = 0; index < request.installments().size(); index++) {
            AdminPaymentPlanCreateRequest.Installment installment = request.installments().get(index);
            if (previousDueDate != null && !installment.dueDate().isAfter(previousDueDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Installment due dates must be in ascending order");
            }
            previousDueDate = installment.dueDate();
            total = total.add(installment.amountDue());
        }
        if (total.compareTo(contract.purchasePrice()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Installment total must equal the purchase price");
        }

        NewPaymentPlan plan = new NewPaymentPlan();
        plan.setPurchaseContractId(contract.contractId());
        plan.setPlanName(request.planName().trim());
        plan.setInstallmentCount(request.installments().size());
        plan.setTotalAmount(total);
        plan.setStartDate(request.startDate());
        if (mapper.insertPaymentPlan(plan) != 1 || plan.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment plan could not be created");
        }

        for (int index = 0; index < request.installments().size(); index++) {
            AdminPaymentPlanCreateRequest.Installment source = request.installments().get(index);
            NewPaymentInstallment installment = new NewPaymentInstallment();
            installment.setPaymentPlanId(plan.getId());
            installment.setInstallmentNo(index + 1);
            installment.setMilestone(normalize(source.milestone()));
            installment.setDueDate(source.dueDate());
            installment.setAmountDue(source.amountDue());
            if (mapper.insertPaymentInstallment(installment) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Payment installment could not be created");
            }
        }
        return new AdminPaymentPlanCreateResponse(plan.getId(), contract.contractId(), plan.getPlanName(),
                plan.getInstallmentCount(), plan.getTotalAmount());
    }

    @Transactional
    public void updateInstallment(Long installmentId, AdminInstallmentUpdateRequest request) {
        InstallmentActionContext context = mapper.lockInstallment(installmentId);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active payment installment not found");
        }
        if (zero(context.getAmountPaid()).compareTo(zero(context.getAmountDue())) >= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed installment cannot be edited");
        }
        if (mapper.updateInstallment(installmentId, normalize(request.milestone()), request.dueDate()) != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment installment could not be updated");
        }
    }

    @Transactional
    public void sendPaymentReminder(Long installmentId) {
        ReminderContext context = mapper.findReminderContext(installmentId);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active payment installment not found");
        }
        BigDecimal unpaid = zero(context.getAmountDue()).subtract(zero(context.getAmountPaid())).max(BigDecimal.ZERO);
        if (unpaid.signum() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed installment does not need a reminder");
        }
        if (mapper.countRecentReminders(installmentId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A reminder was already sent for this installment within the last hour");
        }
        boolean overdue = context.getDueDate() != null && context.getDueDate().isBefore(LocalDate.now());
        String stage = normalize(context.getMilestone()) == null ? "付款期數" : context.getMilestone().trim();
        String title = overdue ? "房款逾期提醒" : "房款付款提醒";
        String body = "%s %s · %s，%s尚有 RM %s 未繳，到期日 %s。".formatted(
                context.getProjectName(), context.getUnitNo(), stage, overdue ? "已逾期，" : "",
                unpaid.setScale(2).toPlainString(), context.getDueDate());
        if (mapper.insertPaymentReminder(context.getUserId(), context.getOwnerId(), installmentId,
                title, body, overdue ? "high" : "normal") != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment reminder could not be sent");
        }
    }

    @Transactional(readOnly = true)
    public AdminBuildingPaymentResponse findPaymentProgress() {
        return findPaymentProgress(1, 10, null, null, null);
    }

    @Transactional(readOnly = true)
    public AdminBuildingPaymentResponse findPaymentProgress(int requestedPage, int requestedPageSize,
                                                             String keyword, String projectName, String status) {
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedKeyword = normalize(keyword);
        String normalizedProject = normalize(projectName);
        String normalizedStatus = normalizeStatus(status);
        long totalRows = zero(mapper.countPageRows(normalizedKeyword, normalizedProject, normalizedStatus));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<InstallmentRow> source = mapper.findPageRows(normalizedKeyword, normalizedProject, normalizedStatus,
                pageSize, (page - 1) * pageSize);
        List<InstallmentRow> summarySource = mapper.findRows();
        Map<Long, BigDecimal> contractTotals = new LinkedHashMap<>();
        BigDecimal paidTotal = BigDecimal.ZERO;
        BigDecimal unpaidTotal = BigDecimal.ZERO;
        int paidInstallments = 0;
        InstallmentRow next = null;

        for (InstallmentRow row : summarySource) {
            if (row.getContractId() != null) {
                contractTotals.putIfAbsent(row.getContractId(), zero(row.getPurchasePrice()));
            }
            paidTotal = paidTotal.add(zero(row.getAmountPaid()));
            unpaidTotal = unpaidTotal.add(zero(row.getUnpaidAmount()));
            if ("paid".equals(row.getStatus())) paidInstallments++;
            if (row.getUnpaidAmount() != null && row.getUnpaidAmount().signum() > 0
                    && (next == null || compareDueDate(row, next) < 0)) next = row;
        }

        AdminBuildingPaymentResponse.Summary summary = new AdminBuildingPaymentResponse.Summary(
                contractTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                paidTotal,
                unpaidTotal,
                summarySource.size(),
                paidInstallments,
                summarySource.size() - paidInstallments,
                next == null ? BigDecimal.ZERO : zero(next.getUnpaidAmount()),
                next == null ? null : next.getDueDate());

        List<AdminBuildingPaymentResponse.Installment> rows = source.stream().map(this::toResponse).toList();
        return new AdminBuildingPaymentResponse(summary, rows,
                new AdminBuildingPaymentResponse.Page(totalRows, page, pageSize, totalPages));
    }

    private AdminBuildingPaymentResponse.Installment toResponse(InstallmentRow row) {
        return new AdminBuildingPaymentResponse.Installment(
                row.getId(), row.getPaymentPlanId(), row.getContractId(), row.getProjectName(), row.getUnitNo(),
                row.getOwnerName(), row.getContractNo(), row.getPlanName(), row.getInstallmentNo(), row.getMilestone(),
                row.getDueDate(), zero(row.getAmountDue()), zero(row.getAmountPaid()), zero(row.getUnpaidAmount()),
                row.getPaymentDate(), row.getStatus(), row.getReceiptNo(), zero(row.getPurchasePrice()),
                row.getFinanceRecordId(), row.getConfirmationStatus(), row.getPaymentMethod(), row.getBankReference(),
                row.getSubmissionNote(), row.getProofDocumentId(), zero(row.getSubmittedAmount()));
    }

    private int compareDueDate(InstallmentRow left, InstallmentRow right) {
        LocalDate leftDate = left.getDueDate();
        LocalDate rightDate = right.getDueDate();
        if (leftDate == null) return 1;
        if (rightDate == null) return -1;
        return leftDate.compareTo(rightDate);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private long zero(Long value) { return value == null ? 0 : value; }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String value) {
        String normalized = normalize(value);
        return normalized != null && switch (normalized) {
            case "paid", "partial", "pending", "overdue" -> true;
            default -> false;
        } ? normalized : null;
    }
}
