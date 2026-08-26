package com.ccps.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.ccps.backend.mapper.WhatsAppNotificationMapper;
import com.ccps.backend.mapper.WhatsAppNotificationMapper.NewAttempt;
import com.ccps.backend.mapper.WhatsAppNotificationMapper.WhatsAppDeliveryRow;
import com.ccps.backend.service.WhatsAppGraphClient.SendResult;
import com.ccps.backend.service.WhatsAppGraphClient.WhatsAppGraphException;
import com.ccps.backend.service.WhatsAppTemplateCatalog.Template;

@Service
public class WhatsAppNotificationService {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationService.class);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final WhatsAppNotificationMapper mapper;
    private final WhatsAppGraphClient graphClient;
    private final WhatsAppTemplateCatalog templates;
    private final String defaultCountryCode;

    public WhatsAppNotificationService(
            WhatsAppNotificationMapper mapper,
            WhatsAppGraphClient graphClient,
            WhatsAppTemplateCatalog templates,
            @Value("${ccps.whatsapp.default-country-code:60}") String defaultCountryCode) {
        this.mapper = mapper;
        this.graphClient = graphClient;
        this.templates = templates;
        this.defaultCountryCode = defaultCountryCode;
    }

    @Scheduled(initialDelayString = "${ccps.whatsapp.delivery-initial-delay-ms:15000}",
            fixedDelayString = "${ccps.whatsapp.delivery-delay-ms:15000}")
    public void deliverPendingNotifications() {
        if (!graphClient.configured()) return;
        for (WhatsAppDeliveryRow delivery : mapper.findPendingDeliveries()) deliver(delivery);
        for (WhatsAppDeliveryRow delivery : mapper.findPendingLeaseExpiryDeliveries()) deliver(delivery);
    }

    void deliver(WhatsAppDeliveryRow delivery) {
        Template template;
        try {
            template = templates.templateFor(delivery.getStage());
        } catch (RuntimeException exception) {
            failWithoutAttempt(delivery.getDeliveryId(), exception.getMessage());
            return;
        }
        if (mapper.claim(delivery.getDeliveryId()) != 1) return;

        NewAttempt attempt = new NewAttempt();
        mapper.insertAttempt(delivery.getDeliveryId(), template.name(), template.language(), attempt);
        try {
            String destination = WhatsAppPhoneNumbers.normalize(delivery.getDestination(), defaultCountryCode);
            SendResult result = graphClient.sendTemplate(destination, template.name(), template.language(),
                    parameters(delivery));
            mapper.markAttemptAccepted(attempt.getId(), result.messageId(), result.waId());
            mapper.markDeliveryAccepted(delivery.getDeliveryId());
        } catch (WhatsAppGraphException exception) {
            String status = exception.uncertain() ? "unknown" : "failed";
            mapper.markAttemptFailed(attempt.getId(), status, exception.code(), exception.subcode(),
                    safe(exception.getMessage()), exception.traceId());
            mapper.markDeliveryFailed(delivery.getDeliveryId(), status, safe(exception.getMessage()));
            log.warn("WhatsApp delivery {} ended as {}: {}", delivery.getDeliveryId(), status, safe(exception.getMessage()));
        } catch (RuntimeException exception) {
            mapper.markAttemptFailed(attempt.getId(), "failed", null, null, safe(exception.getMessage()), null);
            mapper.markDeliveryFailed(delivery.getDeliveryId(), "failed", safe(exception.getMessage()));
            log.warn("WhatsApp delivery {} failed: {}", delivery.getDeliveryId(), safe(exception.getMessage()));
        }
    }

    private List<String> parameters(WhatsAppDeliveryRow row) {
        if ("lease_expiry_business".equals(row.getStage())) {
            return List.of(
                    text(row.getTenantName(), "业务人员"),
                    (text(row.getProjectName(), "房产") + " " + text(row.getUnitNo(), "")).trim(),
                    text(row.getLeaseNo(), "—"),
                    row.getDueDate() == null ? "—" : row.getDueDate().toString());
        }
        return List.of(
                text(row.getTenantName(), "Tenant"),
                (text(row.getProjectName(), "Property") + " " + text(row.getUnitNo(), "")).trim(),
                row.getBillingMonth() == null ? "—" : MONTH.format(row.getBillingMonth()),
                "RM " + money(row.getOutstandingAmount()),
                row.getDueDate() == null ? "—" : row.getDueDate().toString(),
                String.valueOf(row.getOverdueDays() == null ? 0 : row.getOverdueDays()),
                stageLabel(row.getStage()));
    }

    private void failWithoutAttempt(Long deliveryId, String reason) {
        if (mapper.claim(deliveryId) != 1) return;
        mapper.markDeliveryFailed(deliveryId, "failed", safe(reason));
    }

    private String stageLabel(String stage) {
        return switch (stage) {
            case "first_reminder" -> "第一次提醒";
            case "second_reminder" -> "第二次提醒";
            case "final_reminder" -> "最终提醒";
            case "termination_notice" -> "终止通知";
            default -> stage == null ? "催缴提醒" : stage;
        };
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String safe(String value) {
        String result = value == null ? "Unknown WhatsApp delivery error" : value.replaceAll("[\\r\\n]+", " ").trim();
        return result.length() <= 500 ? result : result.substring(0, 500);
    }
}
