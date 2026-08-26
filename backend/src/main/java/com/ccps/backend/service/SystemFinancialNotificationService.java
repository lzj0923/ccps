package com.ccps.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.SystemFinancialNotificationMapper;
import com.ccps.backend.mapper.SystemFinancialNotificationMapper.BuildingPaymentRow;
import com.ccps.backend.mapper.SystemFinancialNotificationMapper.NewNotification;
import com.ccps.backend.mapper.SystemFinancialNotificationMapper.ReserveRow;

@Service
public class SystemFinancialNotificationService {
    private static final Stage BUILDING_DUE_7D = new Stage("due_7d", 7, "房款即将到期", "high");
    private static final Stage BUILDING_DUE_TODAY = new Stage("due_today", 0, "房款今日到期", "high");
    private static final Stage BUILDING_OVERDUE_1D = new Stage("overdue_1d", -1, "房款逾期提醒", "urgent");

    private final SystemFinancialNotificationMapper mapper;
    private final Clock clock;

    @Autowired
    public SystemFinancialNotificationService(SystemFinancialNotificationMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    SystemFinancialNotificationService(SystemFinancialNotificationMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BuildingTask> dueBuildingTasks() {
        LocalDate today = LocalDate.now(clock);
        return mapper.findBuildingPaymentsDue(today).stream()
                .map(row -> task(row, today))
                .filter(task -> task != null)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> dueReserveAccounts() {
        LocalDateTime repeatBefore = LocalDateTime.now(clock).minusDays(7);
        return mapper.findLowReserves().stream()
                .filter(row -> row.getLastSentAt() == null || !row.getLastSentAt().isAfter(repeatBefore))
                .map(ReserveRow::getRelatedId)
                .toList();
    }

    @Transactional
    public void sendBuildingNotice(Long relatedId, String requestedStage) {
        BuildingPaymentRow row = mapper.lockBuildingPayment(relatedId);
        if (row == null || row.getDueDate() == null || outstanding(row).signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "房款分期已结清或不可用");
        }
        Stage stage = desiredBuildingStage(row.getDueDate(), LocalDate.now(clock));
        if (stage == null || !stage.code().equals(requestedStage) || sentStages(row).contains(stage.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前房款提醒阶段无需发送");
        }
        String periodKey = row.getDueDate().toString();
        if (mapper.countSentAction("building_payment", relatedId, stage.code(), periodKey) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前房款提醒已发送");
        }
        String body = "%s业主您好：%s %s 的%s尚有 RM %s 未缴，到期日 %s。".formatted(
                name(row.getRecipientName()), name(row.getProjectName()), name(row.getUnitNo()),
                name(row.getLabel()), money(outstanding(row)), row.getDueDate());
        Long notificationId = createNotification(row.getRecipientUserId(), row.getRecipientOwnerId(),
                row.getRecipientEmail(), stage.title(), body, "payment_installment", relatedId, stage.priority());
        LocalDate scheduledDate = row.getDueDate().minusDays(stage.daysBefore());
        if (mapper.insertAction("building_payment", relatedId, stage.code(), periodKey, scheduledDate,
                notificationId, stage.title(), body) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "房款提醒记录建立失败");
        }
        mapper.insertAudit("payment_installment", relatedId, stage.code(), periodKey, stage.title());
    }

    @Transactional
    public void sendReserveNotice(Long relatedId) {
        ReserveRow row = mapper.lockReserve(relatedId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (row == null || !Boolean.TRUE.equals(row.getLowBalanceAlertEnabled())
                || moneyValue(row.getCurrentBalance()).compareTo(moneyValue(row.getMinimumBalance())) >= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "预备金余额已恢复或提醒已停用");
        }
        if (row.getLastSentAt() != null && row.getLastSentAt().isAfter(now.minusDays(7))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "预备金不足提醒七天内已发送");
        }
        String periodKey = LocalDate.now(clock).toString();
        if (mapper.countSentAction("reserve", relatedId, "reserve_low", periodKey) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "今日预备金不足提醒已发送");
        }
        BigDecimal shortage = moneyValue(row.getMinimumBalance()).subtract(moneyValue(row.getCurrentBalance()))
                .max(BigDecimal.ZERO);
        String title = "预备金低于最低标准";
        String body = "%s业主您好：%s %s 的预备金余额为 RM %s，最低标准为 RM %s，尚需补足 RM %s。".formatted(
                name(row.getRecipientName()), name(row.getProjectName()), name(row.getUnitNo()),
                money(row.getCurrentBalance()), money(row.getMinimumBalance()), money(shortage));
        Long notificationId = createNotification(row.getRecipientUserId(), row.getRecipientOwnerId(),
                row.getRecipientEmail(), title, body, "reserve_account", relatedId, "high");
        if (mapper.insertAction("reserve", relatedId, "reserve_low", periodKey, LocalDate.now(clock),
                notificationId, title, body) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "预备金提醒记录建立失败");
        }
        mapper.insertAudit("reserve_account", relatedId, "reserve_low", periodKey, title);
    }

    private BuildingTask task(BuildingPaymentRow row, LocalDate today) {
        Stage stage = desiredBuildingStage(row.getDueDate(), today);
        if (stage == null || sentStages(row).contains(stage.code())) return null;
        return new BuildingTask(row.getRelatedId(), stage.code());
    }

    private Stage desiredBuildingStage(LocalDate dueDate, LocalDate today) {
        if (dueDate == null) return null;
        long daysUntilDue = ChronoUnit.DAYS.between(today, dueDate);
        if (daysUntilDue <= BUILDING_OVERDUE_1D.daysBefore()) return BUILDING_OVERDUE_1D;
        if (daysUntilDue <= BUILDING_DUE_TODAY.daysBefore()) return BUILDING_DUE_TODAY;
        if (daysUntilDue <= BUILDING_DUE_7D.daysBefore()) return BUILDING_DUE_7D;
        return null;
    }

    private Set<String> sentStages(BuildingPaymentRow row) {
        if (row.getSentStages() == null || row.getSentStages().isBlank()) return Set.of();
        return new LinkedHashSet<>(Arrays.asList(row.getSentStages().split(",")));
    }

    private Long createNotification(Long userId, Long ownerId, String email, String title, String body,
                                    String relatedType, Long relatedId, String priority) {
        NewNotification notification = new NewNotification();
        notification.setRecipientUserId(userId); notification.setRecipientOwnerId(ownerId);
        notification.setTitle(title); notification.setBody(body); notification.setRelatedType(relatedType);
        notification.setRelatedId(relatedId); notification.setPriority(priority);
        if (mapper.insertNotification(notification) != 1 || notification.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "系统财务通知建立失败");
        }
        mapper.insertInAppDelivery(notification.getId());
        if (email != null && !email.isBlank()) {
            mapper.insertEmailDelivery(notification.getId(), email.trim().toLowerCase());
        }
        return notification.getId();
    }

    private BigDecimal outstanding(BuildingPaymentRow row) {
        return moneyValue(row.getAmountDue()).subtract(moneyValue(row.getAmountPaid())).max(BigDecimal.ZERO);
    }

    private BigDecimal moneyValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        return moneyValue(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String name(String value) { return value == null || value.isBlank() ? "—" : value.trim(); }

    public record BuildingTask(Long relatedId, String stage) { }
    private record Stage(String code, int daysBefore, String title, String priority) { }
}
