package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.ccps.backend.dto.AdminRentCollectionWorkflowResponse;
import com.ccps.backend.mapper.AdminRentCollectionWorkflowMapper;
import com.ccps.backend.mapper.AdminRentCollectionWorkflowMapper.CollectionRow;

@ExtendWith(MockitoExtension.class)
class AdminRentCollectionWorkflowServiceTest {
    @Mock
    private AdminRentCollectionWorkflowMapper mapper;

    private AdminRentCollectionWorkflowService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:00:00Z"), ZoneOffset.UTC);
        service = new AdminRentCollectionWorkflowService(mapper, clock);
    }

    @Test
    void springCanCreateServiceWithMapperConstructor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(AdminRentCollectionWorkflowMapper.class, () -> mapper);
            context.register(AdminRentCollectionWorkflowService.class);
            context.refresh();

            assertThat(context.getBean(AdminRentCollectionWorkflowService.class)).isNotNull();
        }
    }

    @Test
    void calculatesFourStagesFromEachInvoiceDueDate() {
        when(mapper.findOutstanding(LocalDate.parse("2026-08-10"))).thenReturn(List.of(
                row(1L, "2026-07-27", null),
                row(2L, "2026-07-25", "first_reminder"),
                row(3L, "2026-07-23", "first_reminder,second_reminder"),
                row(4L, "2026-07-21", "first_reminder,second_reminder,final_reminder")));

        AdminRentCollectionWorkflowResponse result = service.overview();

        assertThat(result.items()).extracting(AdminRentCollectionWorkflowResponse.Item::currentStage)
                .containsExactly("first_reminder", "second_reminder", "final_reminder", "termination_notice");
        assertThat(result.summary().outstandingAmount()).isEqualByComparingTo("3200.00");
        assertThat(result.items().get(0).scheduledDate()).isEqualTo(LocalDate.parse("2026-08-10"));
    }

    @Test
    void refusesToSkipAnEarlierDueStage() {
        CollectionRow row = row(9L, "2026-07-21", null);
        when(mapper.lockInvoice(9L)).thenReturn(row);

        assertThatThrownBy(() -> service.sendStage(7L, 9L, "termination_notice"))
                .hasMessageContaining("currently due collection stage");
    }

    @Test
    void queuesWhatsAppWhenTenantHasExplicitlyOptedIn() {
        CollectionRow row = row(11L, "2026-07-27", null);
        row.setWhatsappEnabled(true);
        row.setWhatsappDestination("60123456789");
        when(mapper.lockInvoice(11L)).thenReturn(row);
        when(mapper.countSentStage(11L, "first_reminder")).thenReturn(0);
        doAnswer(invocation -> {
            AdminRentCollectionWorkflowMapper.NewNotification notification = invocation.getArgument(0);
            notification.setId(88L);
            return 1;
        }).when(mapper).insertNotification(any());
        when(mapper.insertAction(eq(11L), eq("first_reminder"), eq(14), any(), any(), any(),
                eq(88L), eq(7L))).thenReturn(1);

        AdminRentCollectionWorkflowResponse.Item result = service.sendStage(7L, 11L, "first_reminder");

        assertThat(result.whatsappEnabled()).isTrue();
        assertThat(result.whatsappDestination()).isEqualTo("60123456789");
        verify(mapper).insertWhatsAppDelivery(88L, 211L);
    }

    @Test
    void collectionNoticeUsesTheActualInvoiceOverdueDays() {
        CollectionRow row = row(12L, "2026-07-21", null);
        when(mapper.lockInvoice(12L)).thenReturn(row);
        when(mapper.countSentStage(12L, "first_reminder")).thenReturn(0);
        doAnswer(invocation -> {
            AdminRentCollectionWorkflowMapper.NewNotification notification = invocation.getArgument(0);
            notification.setId(89L);
            return 1;
        }).when(mapper).insertNotification(any());
        when(mapper.insertAction(eq(12L), eq("first_reminder"), eq(14), any(), any(), any(),
                eq(89L), eq(7L))).thenReturn(1);

        service.sendStage(7L, 12L, "first_reminder");

        ArgumentCaptor<AdminRentCollectionWorkflowMapper.NewNotification> captor =
                ArgumentCaptor.forClass(AdminRentCollectionWorkflowMapper.NewNotification.class);
        verify(mapper).insertNotification(captor.capture());
        assertThat(captor.getValue().getBody()).contains("已逾期 20 天");
    }

    @Test
    void pausingACollectionRequiresARecordedReason() {
        CollectionRow row = row(10L, "2026-07-27", null);
        CollectionRow held = row(10L, "2026-07-27", null);
        held.setWorkflowStatus("on_hold");
        held.setHoldReason("付款核对中");
        when(mapper.lockInvoice(10L)).thenReturn(row, held);
        when(mapper.hold(10L, 7L, "付款核对中")).thenReturn(1);

        AdminRentCollectionWorkflowResponse.Item result = service.hold(7L, 10L, "付款核对中");

        assertThat(result.workflowStatus()).isEqualTo("on_hold");
        assertThat(result.holdReason()).isEqualTo("付款核对中");
        verify(mapper).insertAudit(eq(7L), eq(10L), eq("hold_rent_collection"), eq(null), eq("付款核对中"));
    }

    private CollectionRow row(Long id, String dueDate, String sentStages) {
        CollectionRow row = new CollectionRow();
        row.setInvoiceId(id); row.setLeaseId(100L + id); row.setTenantId(200L + id);
        row.setTenantUserId(300L + id); row.setTenantName("测试租客"); row.setTenantPhone("0123456789");
        row.setTenantEmail("tenant@example.com"); row.setProjectName("测试建案"); row.setUnitNo("A-01");
        row.setLeaseNo("LEASE-" + id); row.setLeaseStatus("active");
        row.setBillingMonth(LocalDate.parse("2026-07-01")); row.setDueDate(LocalDate.parse(dueDate));
        row.setAmountDue(new BigDecimal("1000.00")); row.setAmountPaid(new BigDecimal("200.00"));
        row.setWorkflowStatus("active"); row.setSentStages(sentStages);
        return row;
    }
}
