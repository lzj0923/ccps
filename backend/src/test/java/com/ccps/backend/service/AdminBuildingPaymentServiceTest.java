package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPaymentContractOption;
import com.ccps.backend.dto.AdminInstallmentUpdateRequest;
import com.ccps.backend.dto.AdminPaymentPlanCreateRequest;
import com.ccps.backend.dto.AdminPaymentPlanCreateResponse;
import com.ccps.backend.dto.AdminProjectCreateRequest;
import com.ccps.backend.dto.AdminProjectOption;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.NewPaymentInstallment;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.NewPaymentPlan;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.NewProject;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.InstallmentActionContext;
import com.ccps.backend.mapper.AdminBuildingPaymentMapper.ReminderContext;

@ExtendWith(MockitoExtension.class)
class AdminBuildingPaymentServiceTest {
    @Mock private AdminBuildingPaymentMapper mapper;
    private AdminBuildingPaymentService service;

    @BeforeEach
    void setUp() {
        service = new AdminBuildingPaymentService(mapper);
    }

    @Test
    void createsNormalizedProject() {
        AdminProjectCreateRequest request = new AdminProjectCreateRequest(
                " ccps-01 ", " New Project ", "Address", "Kuala Lumpur", "Bukit Bintang", "MY", "active");
        when(mapper.insertProject(any(NewProject.class))).thenAnswer(invocation -> {
            NewProject project = invocation.getArgument(0);
            project.setId(7L);
            return 1;
        });

        AdminProjectOption result = service.createProject(request);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.code()).isEqualTo("CCPS-01");
        assertThat(result.name()).isEqualTo("New Project");
    }

    @Test
    void createsPlanAndAllInstallments() {
        AdminPaymentPlanCreateRequest request = request("400000.00", "600000.00");
        when(mapper.lockPurchaseContract(12L)).thenReturn(12L);
        when(mapper.findPaymentContract(12L)).thenReturn(contract("1000000.00"));
        when(mapper.insertPaymentPlan(any(NewPaymentPlan.class))).thenAnswer(invocation -> {
            NewPaymentPlan plan = invocation.getArgument(0);
            plan.setId(88L);
            return 1;
        });
        when(mapper.insertPaymentInstallment(any(NewPaymentInstallment.class))).thenReturn(1);

        AdminPaymentPlanCreateResponse result = service.createPaymentPlan(request);

        assertThat(result.id()).isEqualTo(88L);
        assertThat(result.installmentCount()).isEqualTo(2);
        assertThat(result.totalAmount()).isEqualByComparingTo("1000000.00");
        verify(mapper, org.mockito.Mockito.times(2)).insertPaymentInstallment(any(NewPaymentInstallment.class));
    }

    @Test
    void rejectsPlanWhenInstallmentsDoNotEqualContractPrice() {
        when(mapper.lockPurchaseContract(12L)).thenReturn(12L);
        when(mapper.findPaymentContract(12L)).thenReturn(contract("1000000.00"));

        assertThatThrownBy(() -> service.createPaymentPlan(request("400000.00", "500000.00")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("must equal the purchase price");
        verify(mapper, never()).insertPaymentPlan(any(NewPaymentPlan.class));
    }

    @Test
    void rejectsSecondActivePlanForSameContract() {
        when(mapper.lockPurchaseContract(12L)).thenReturn(12L);
        when(mapper.findPaymentContract(12L)).thenReturn(contract("1000000.00"));
        when(mapper.countActivePaymentPlans(12L)).thenReturn(1);

        assertThatThrownBy(() -> service.createPaymentPlan(request("400000.00", "600000.00")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void updatesAnUnpaidInstallmentStageAndDueDate() {
        InstallmentActionContext context = installmentContext("5000.00", "0.00");
        LocalDate dueDate = LocalDate.of(2026, 10, 1);
        when(mapper.lockInstallment(31L)).thenReturn(context);
        when(mapper.updateInstallment(31L, "Foundation complete", dueDate)).thenReturn(1);

        service.updateInstallment(31L, new AdminInstallmentUpdateRequest(" Foundation complete ", dueDate));

        verify(mapper).updateInstallment(31L, "Foundation complete", dueDate);
    }

    @Test
    void rejectsEditingACompletedInstallment() {
        when(mapper.lockInstallment(31L)).thenReturn(installmentContext("5000.00", "5000.00"));

        assertThatThrownBy(() -> service.updateInstallment(31L,
                new AdminInstallmentUpdateRequest("Completed", LocalDate.of(2026, 10, 1))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot be edited");
    }

    @Test
    void sendsAnOwnerPaymentReminder() {
        ReminderContext context = new ReminderContext();
        context.setId(31L);
        context.setDueDate(LocalDate.now().plusDays(10));
        context.setAmountDue(new BigDecimal("5000.00"));
        context.setAmountPaid(new BigDecimal("1000.00"));
        context.setMilestone("Foundation");
        context.setProjectName("Pavilion Square");
        context.setUnitNo("A-01");
        context.setOwnerId(8L);
        context.setUserId(18L);
        when(mapper.findReminderContext(31L)).thenReturn(context);
        when(mapper.insertPaymentReminder(18L, 8L, 31L, "房款付款提醒",
                "Pavilion Square A-01 · Foundation，尚有 RM 4000.00 未繳，到期日 " + context.getDueDate() + "。",
                "normal")).thenReturn(1);

        service.sendPaymentReminder(31L);

        verify(mapper).insertPaymentReminder(18L, 8L, 31L, "房款付款提醒",
                "Pavilion Square A-01 · Foundation，尚有 RM 4000.00 未繳，到期日 " + context.getDueDate() + "。",
                "normal");
    }

    private AdminPaymentPlanCreateRequest request(String first, String second) {
        return new AdminPaymentPlanCreateRequest(12L, "Progress plan", LocalDate.of(2026, 7, 19), List.of(
                new AdminPaymentPlanCreateRequest.Installment("Deposit", LocalDate.of(2026, 8, 1), new BigDecimal(first)),
                new AdminPaymentPlanCreateRequest.Installment("Foundation", LocalDate.of(2026, 9, 1), new BigDecimal(second))));
    }

    private AdminPaymentContractOption contract(String price) {
        return new AdminPaymentContractOption(12L, 22L, 7L, "Pavilion Square", "A-01", "Test Owner",
                "PC-001", new BigDecimal(price), "MYR", false);
    }

    private InstallmentActionContext installmentContext(String due, String paid) {
        InstallmentActionContext context = new InstallmentActionContext();
        context.setId(31L);
        context.setAmountDue(new BigDecimal(due));
        context.setAmountPaid(new BigDecimal(paid));
        return context;
    }
}
