package com.ccps.backend.service;

final class WhatsAppPhoneNumbers {
    private WhatsAppPhoneNumbers() {
    }

    static String normalize(String input, String defaultCountryCode) {
        String raw = input == null ? "" : input.trim();
        if (raw.isBlank()) throw new IllegalArgumentException("WhatsApp phone number is required");

        boolean international = raw.startsWith("+");
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
            international = true;
        }
        if (!international && digits.startsWith("0")) {
            String country = defaultCountryCode == null ? "" : defaultCountryCode.replaceAll("\\D", "");
            if (country.isBlank()) {
                throw new IllegalArgumentException("A default country code is required for local WhatsApp numbers");
            }
            digits = country + digits.substring(1);
        }
        if (!digits.matches("[1-9][0-9]{7,14}")) {
            throw new IllegalArgumentException("WhatsApp phone number must use a valid international format");
        }
        return digits;
    }
}
