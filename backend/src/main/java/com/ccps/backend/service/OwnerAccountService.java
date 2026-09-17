package com.ccps.backend.service;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.ccps.backend.dto.OwnerAccountResponse;
import com.ccps.backend.mapper.OwnerAccountMapper;

@Service
public class OwnerAccountService {
    private final OwnerAccountMapper mapper;
    private final PortalSessionService sessions;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public OwnerAccountService(OwnerAccountMapper mapper, PortalSessionService sessions) {
        this.mapper = mapper; this.sessions = sessions;
    }
    public OwnerAccountResponse get(Long userId) {
        OwnerAccountResponse owner = mapper.find(userId);
        if (owner == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner profile not found");
        return owner;
    }
    public OwnerAccountResponse update(Long userId, Contact request) {
        OwnerAccountResponse current = get(userId);
        if (request == null) throw invalid("Contact information is required");
        OwnerAccountResponse contact = new OwnerAccountResponse(current.fullName(), phone(request.mobilePhone()),
                phone(request.homePhone()), phone(request.officePhone()), text(request.mailingAddress(), 500));
        if (mapper.updateContact(userId, contact) != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Owner profile changed; refresh and retry");
        return contact;
    }
    public void changePassword(Long userId, Password request) {
        get(userId);
        if (request == null || request.currentPassword() == null || request.currentPassword().isEmpty()
                || request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72) throw invalid("Current password is required");
        String next = request.newPassword();
        if (next == null || next.length() < 10 || next.getBytes(StandardCharsets.UTF_8).length > 72
                || !next.matches("(?s).*[A-Za-z].*") || !next.matches("(?s).*\\d.*"))
            throw invalid("New password must contain letters and numbers, at least 10 characters and at most 72 bytes");
        String previous = mapper.passwordHash(userId);
        if (previous == null || !encoder.matches(request.currentPassword(), previous)) throw invalid("Current password is incorrect");
        if (encoder.matches(next, previous)) throw invalid("New password must be different");
        if (mapper.changePassword(userId, previous, encoder.encode(next)) != 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Password changed; sign in again");
        sessions.revokeOwnerSessions(userId);
    }
    private String phone(String value) {
        String normalized = text(value, 40);
        if (!normalized.isEmpty() && !normalized.matches("\\+?[0-9() .-]{5,40}")) throw invalid("Invalid contact phone number");
        return normalized;
    }
    private String text(String value, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > max || normalized.indexOf('\0') >= 0) throw invalid("Contact information is too long or invalid");
        return normalized;
    }
    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    public record Contact(String mobilePhone, String homePhone, String officePhone, String mailingAddress) { }
    public record Password(String currentPassword, String newPassword) { }
}
