package com.ccps.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppTemplateCatalog {
    private final String language;
    private final String firstReminder;
    private final String secondReminder;
    private final String finalReminder;
    private final String terminationNotice;
    private final String leaseExpiryBusiness;

    public WhatsAppTemplateCatalog(
            @Value("${ccps.whatsapp.templates.language:zh_CN}") String language,
            @Value("${ccps.whatsapp.templates.first-reminder:ccps_rent_collection_notice}") String firstReminder,
            @Value("${ccps.whatsapp.templates.second-reminder:ccps_rent_collection_notice}") String secondReminder,
            @Value("${ccps.whatsapp.templates.final-reminder:ccps_rent_collection_notice}") String finalReminder,
            @Value("${ccps.whatsapp.templates.termination-notice:ccps_rent_collection_notice}") String terminationNotice,
            @Value("${ccps.whatsapp.templates.lease-expiry-business:ccps_lease_expiry_business_notice}") String leaseExpiryBusiness) {
        this.language = language == null ? "" : language.trim();
        this.firstReminder = normalize(firstReminder);
        this.secondReminder = normalize(secondReminder);
        this.finalReminder = normalize(finalReminder);
        this.terminationNotice = normalize(terminationNotice);
        this.leaseExpiryBusiness = normalize(leaseExpiryBusiness);
    }

    public Template templateFor(String stage) {
        String name = switch (stage) {
            case "first_reminder" -> firstReminder;
            case "second_reminder" -> secondReminder;
            case "final_reminder" -> finalReminder;
            case "termination_notice" -> terminationNotice;
            case "lease_expiry_business" -> leaseExpiryBusiness;
            default -> "";
        };
        if (name.isBlank() || language.isBlank()) {
            throw new IllegalStateException("WhatsApp template is not configured for collection stage " + stage);
        }
        return new Template(name, language);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record Template(String name, String language) {
    }
}
