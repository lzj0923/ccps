package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminReminderResponse;
import com.ccps.backend.dto.AdminReminderRuleRequest;
import com.ccps.backend.mapper.AdminReminderMapper;
import com.ccps.backend.mapper.AdminReminderMapper.EventContext;
import com.ccps.backend.mapper.AdminReminderMapper.DeliveryRow;
import com.ccps.backend.mapper.AdminReminderMapper.NewNotification;
import com.ccps.backend.mapper.AdminReminderMapper.NotificationRow;
import com.ccps.backend.mapper.AdminReminderMapper.RuleRow;
import com.ccps.backend.mapper.AdminReminderMapper.RuleWrite;
import com.ccps.backend.mapper.AdminReminderMapper.SummaryRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AdminReminderService {
    private static final Logger log = LoggerFactory.getLogger(AdminReminderService.class);
    private static final Set<String> CHANNELS = Set.of("in_app", "email", "line", "whatsapp");
    private static final String LEASE_EXPIRY_BUSINESS_RULE = "LEASE_EXPIRY_BUSINESS_30D";
    private final AdminReminderMapper mapper;
    private final ObjectMapper objectMapper;

    public AdminReminderService(AdminReminderMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public AdminReminderResponse overview() {
        SummaryRow summary = mapper.findSummary();
        return new AdminReminderResponse(
                new AdminReminderResponse.Summary(count(summary == null ? null : summary.getRuleCount()),
                        count(summary == null ? null : summary.getEnabledRuleCount()),
                        count(summary == null ? null : summary.getNotificationCount()),
                        count(summary == null ? null : summary.getPendingDeliveryCount()),
                        count(summary == null ? null : summary.getFailedDeliveryCount())),
                mapper.findRules().stream().map(this::toRule).toList(),
                mapper.findNotifications().stream().map(this::toNotification).toList(),
                mapper.findDeliveries().stream().map(this::toDelivery).toList());
    }

    @Transactional
    public AdminReminderResponse.Rule createRule(Long actorId, AdminReminderRuleRequest request) {
        RuleWrite row = toWrite(null, actorId, request);
        requireNonSystemCode(row.getCode());
        requireUniqueCode(row.getCode(), null);
        if (mapper.insertRule(row) != 1 || row.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reminder rule could not be created");
        }
        mapper.insertRuleAudit(actorId, "create_reminder_rule", row.getId(), row.getCode(), row.getName());
        return toRule(requireRule(row.getId()));
    }

    @Transactional
    public AdminReminderResponse.Rule updateRule(Long actorId, Long ruleId, AdminReminderRuleRequest request) {
        requireUserManagedRule(ruleId);
        RuleWrite row = toWrite(ruleId, actorId, request);
        requireNonSystemCode(row.getCode());
        requireUniqueCode(row.getCode(), ruleId);
        if (mapper.updateRule(row) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reminder rule was not updated");
        }
        mapper.insertRuleAudit(actorId, "update_reminder_rule", ruleId, row.getCode(), row.getName());
        return toRule(requireRule(ruleId));
    }

    @Transactional
    public void setEnabled(Long actorId, Long ruleId, boolean enabled) {
        RuleRow rule = requireUserManagedRule(ruleId);
        if (mapper.setRuleEnabled(ruleId, enabled) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reminder rule state was not updated");
        }
        mapper.insertRuleAudit(actorId, enabled ? "enable_reminder_rule" : "disable_reminder_rule",
                ruleId, rule.getCode(), rule.getName());
    }

    @Transactional
    public void deleteRule(Long actorId, Long ruleId) {
        RuleRow rule = requireUserManagedRule(ruleId);
        if (mapper.deleteUnusedRule(ruleId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Rules with notification history cannot be deleted; disable the rule instead");
        }
        mapper.insertRuleAudit(actorId, "delete_reminder_rule", ruleId, rule.getCode(), rule.getName());
    }

    @Transactional
    public int runRule(Long actorId, Long ruleId) {
        RuleRow rule = requireUserManagedRule(ruleId);
        int created = generateRule(rule);
        mapper.insertRuleAudit(actorId, "run_reminder_rule", ruleId, rule.getCode(), rule.getName());
        return created;
    }

    @Transactional
    public int runEnabledRules(Long actorId) {
        int created = runEnabledRulesInternal(false);
        if (actorId != null) {
            mapper.insertRuleAudit(actorId, "run_all_reminder_rules", 0L, "ALL", "全部啟用規則");
        }
        return created;
    }

    @Scheduled(cron = "${ccps.reminders.cron:0 0 * * * *}")
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runScheduledRules() {
        try {
            int created = runEnabledRulesInternal(true);
            if (created > 0) log.info("Automatic reminders created {} notifications", created);
        } catch (RuntimeException exception) {
            log.error("Automatic reminder schedule failed", exception);
        }
    }

    @Transactional
    public void retryDelivery(Long deliveryId) {
        String channel = mapper.findDeliveryChannel(deliveryId);
        if (channel == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification delivery not found");
        if ("email".equals(channel)) {
            if (mapper.isEmailDeliveryReady(deliveryId) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "The recipient must enable and verify email notifications before retrying");
            }
            if (mapper.retryEmailDelivery(deliveryId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only failed email deliveries can be retried");
            }
            return;
        }
        if ("whatsapp".equals(channel)) {
            int retried = mapper.retryWhatsAppDelivery(deliveryId);
            if (retried == 0) retried = mapper.retryLeaseExpiryWhatsAppDelivery(deliveryId);
            if (retried != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Only failed or unknown WhatsApp deliveries can be retried");
            }
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "This delivery channel cannot be retried");
    }

    private int runEnabledRulesInternal(boolean includeSystemManagedRules) {
        int created = 0;
        for (RuleRow rule : mapper.findRules()) {
            if (isSystemManaged(rule)) {
                if (includeSystemManagedRules) created += generateRule(rule);
            } else if (Boolean.TRUE.equals(rule.getEnabled())) {
                created += generateRule(rule);
            }
        }
        return created;
    }

    private synchronized int generateRule(RuleRow rule) {
        List<EventContext> events = switch (rule.getEventType()) {
            case "payment_due" -> mapper.findPaymentDueEvents(rule.getId(), value(rule.getDaysBefore()));
            case "rent_due" -> mapper.findRentDueEvents(rule.getId(), value(rule.getDaysBefore()));
            case "lease_expiry" -> mapper.findLeaseExpiryEvents(rule.getId(), value(rule.getDaysBefore()));
            case "reserve_low" -> mapper.findReserveLowEvents(rule.getId());
            case "document_expiry" -> mapper.findDocumentExpiryEvents(rule.getId(), value(rule.getDaysBefore()));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported reminder event type: " + rule.getEventType());
        };
        List<String> channels = parseChannels(rule.getChannelsJson());
        int created = 0;
        for (EventContext event : events) {
            Message message = message(rule.getEventType(), event);
            NewNotification notification = new NewNotification();
            notification.setRuleId(rule.getId());
            notification.setRecipientUserId(event.getRecipientUserId());
            notification.setRecipientOwnerId(event.getRecipientOwnerId());
            notification.setRelatedType(event.getRelatedType());
            notification.setRelatedId(event.getRelatedId());
            notification.setTitle(message.title());
            notification.setBody(message.body());
            notification.setPriority(message.priority());
            if (mapper.insertNotification(notification) != 1 || notification.getId() == null) continue;
            createDeliveries(notification.getId(), event, channels);
            created++;
        }
        return created;
    }

    private void createDeliveries(Long notificationId, EventContext event, List<String> channels) {
        if (channels.contains("in_app")) mapper.insertInAppDelivery(notificationId);
        if (channels.contains("email")) {
            int inserted = event.getRecipientUserId() == null ? 0
                    : mapper.insertEmailDelivery(notificationId, event.getRecipientUserId());
            if (inserted == 0) mapper.insertUnavailableEmailDelivery(notificationId, event.getRecipientEmail());
        }
        if (channels.contains("whatsapp")) {
            int inserted = 0;
            if (Boolean.TRUE.equals(event.getWhatsappEnabled())) {
                if (event.getTenantId() != null) {
                    inserted = mapper.insertWhatsAppDelivery(notificationId, event.getTenantId());
                } else if (event.getWhatsappDestination() != null && !event.getWhatsappDestination().isBlank()) {
                    inserted = mapper.insertDirectWhatsAppDelivery(notificationId,
                            event.getWhatsappDestination().trim());
                }
            }
            if (inserted == 0) mapper.insertUnavailableWhatsAppDelivery(notificationId);
        }
        if (channels.contains("line")) mapper.insertUnavailableLineDelivery(notificationId);
    }

    private Message message(String eventType, EventContext event) {
        String property = text(event.getProjectName(), "未設定建案") + " " + text(event.getUnitNo(), "未設定單位");
        LocalDate today = LocalDate.now();
        boolean overdue = event.getDueDate() != null && event.getDueDate().isBefore(today);
        return switch (eventType) {
            case "payment_due" -> new Message(overdue ? "房款逾期提醒" : "房款即將到期",
                    "%s · %s尚有 RM %s 未繳，到期日 %s。".formatted(property,
                            text(event.getLabel(), "房款期數"), money(event.getAmount()), date(event.getDueDate())),
                    overdue ? "urgent" : "high");
            case "rent_due" -> new Message(overdue ? "租金逾期提醒" : "租金即將到期",
                    "%s 的 %s 租金尚有 RM %s 未缴，到期日 %s%s。".formatted(property,
                            text(event.getLabel(), "本期"), money(event.getAmount()), date(event.getDueDate()),
                            overdue ? "，已逾期 " + ChronoUnit.DAYS.between(event.getDueDate(), today) + " 天" : ""),
                    overdue ? "urgent" : "high");
            case "lease_expiry" -> new Message(overdue ? "租約已到期" : "租約即將到期",
                    "%s 的租約 %s 將於 %s%s。".formatted(property, text(event.getLabel(), "—"),
                            date(event.getDueDate()), overdue ? "（已到期）" : "到期"),
                    overdue ? "urgent" : "high");
            case "reserve_low" -> new Message("預備金低於最低標準",
                    "%s 預備金餘額 RM %s，最低標準 RM %s，請安排補繳。".formatted(property,
                            money(event.getAmount()), text(event.getLabel(), "0.00")), "high");
            case "document_expiry" -> new Message(overdue ? "文件已到期" : "文件即將到期",
                    "%s 的文件「%s」將於 %s%s。".formatted(property, text(event.getLabel(), "未命名文件"),
                            date(event.getDueDate()), overdue ? "（已到期）" : "到期"),
                    overdue ? "urgent" : "high");
            default -> throw new IllegalArgumentException("Unsupported reminder event type");
        };
    }

    private RuleWrite toWrite(Long id, Long actorId, AdminReminderRuleRequest request) {
        List<String> channels = request.channels().stream().map(String::trim).map(String::toLowerCase)
                .filter(CHANNELS::contains).distinct().toList();
        if (channels.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one valid channel is required");
        RuleWrite row = new RuleWrite();
        row.setId(id); row.setCreatedBy(actorId); row.setCode(request.code().trim().toUpperCase(Locale.ROOT));
        row.setName(request.name().trim()); row.setEventType(request.eventType()); row.setDaysBefore(request.daysBefore());
        row.setChannelsJson(writeChannels(channels));
        row.setRecipientRole(switch (request.eventType()) {
            case "rent_due" -> "tenant";
            case "lease_expiry" -> "business";
            default -> "owner";
        });
        row.setEnabled(request.enabled());
        return row;
    }

    private AdminReminderResponse.Rule toRule(RuleRow row) {
        return new AdminReminderResponse.Rule(row.getId(), row.getCode(), row.getName(), row.getEventType(),
                value(row.getDaysBefore()), parseChannels(row.getChannelsJson()), row.getRecipientRole(),
                Boolean.TRUE.equals(row.getEnabled()), isSystemManaged(row), row.getCreatedAt(), row.getUpdatedAt());
    }

    private AdminReminderResponse.NotificationItem toNotification(NotificationRow row) {
        return new AdminReminderResponse.NotificationItem(row.getId(), row.getRuleId(), row.getRuleName(),
                row.getEventType(), row.getTitle(), row.getBody(), row.getRecipientName(), row.getRecipientEmail(),
                row.getRelatedType(), row.getRelatedId(), row.getPriority(), row.getNoticeStatus(),
                row.getDeliverySummary(), row.getFailureReason(), row.getCreatedAt());
    }

    private AdminReminderResponse.DeliveryItem toDelivery(DeliveryRow row) {
        return new AdminReminderResponse.DeliveryItem(row.getId(), row.getNotificationId(), row.getRuleName(),
                row.getTitle(), row.getRecipientName(), row.getChannel(), row.getDestination(), row.getStatus(),
                value(row.getAttemptCount()), row.getSentAt(), row.getFailedAt(), row.getFailureReason());
    }

    private RuleRow requireRule(Long ruleId) {
        RuleRow rule = mapper.findRule(ruleId);
        if (rule == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder rule not found");
        return rule;
    }

    private RuleRow requireUserManagedRule(Long ruleId) {
        RuleRow rule = requireRule(ruleId);
        if (isSystemManaged(rule)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "System automatic rules cannot be edited, disabled, deleted, or run manually");
        }
        return rule;
    }

    private void requireNonSystemCode(String code) {
        if (LEASE_EXPIRY_BUSINESS_RULE.equalsIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This rule code is reserved for a system automatic rule");
        }
    }

    private boolean isSystemManaged(RuleRow rule) {
        return rule != null && LEASE_EXPIRY_BUSINESS_RULE.equalsIgnoreCase(rule.getCode());
    }

    private void requireUniqueCode(String code, Long excludeId) {
        if (mapper.countRuleCode(code, excludeId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reminder rule code already exists");
        }
    }

    private String writeChannels(List<String> channels) {
        try { return objectMapper.writeValueAsString(channels); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("Invalid reminder channels", exception); }
    }

    private List<String> parseChannels(String json) {
        if (json == null || json.isBlank()) return List.of("in_app");
        try {
            List<String> parsed = objectMapper.readValue(json, new TypeReference<List<String>>() { });
            return new ArrayList<>(new LinkedHashSet<>(parsed));
        } catch (JsonProcessingException exception) {
            log.warn("Invalid reminder channel JSON: {}", json);
            return List.of("in_app");
        }
    }

    private long count(Long value) { return value == null ? 0 : value; }
    private int value(Integer value) { return value == null ? 0 : value; }
    private String money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2).toPlainString(); }
    private String date(LocalDate value) { return value == null ? "未設定" : value.toString(); }
    private String text(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private record Message(String title, String body, String priority) { }
}
