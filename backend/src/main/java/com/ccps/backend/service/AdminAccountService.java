package com.ccps.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminAccountCreateRequest;
import com.ccps.backend.dto.AdminAccountResponse;
import com.ccps.backend.dto.AdminAccountUpdateRequest;
import com.ccps.backend.mapper.AdminAccountMapper;
import com.ccps.backend.mapper.AdminAccountMapper.AccountRecord;
import com.ccps.backend.mapper.AdminAccountMapper.AccountRow;

@Service
public class AdminAccountService {
    public static final String DEFAULT_OWNER_PASSWORD = "123456";

    private final AdminAccountMapper mapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminAccountService(AdminAccountMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> findAll() {
        return mapper.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdminAccountResponse findById(Long id) {
        return toResponse(require(id));
    }

    @Transactional
    public AdminAccountResponse create(AdminAccountCreateRequest request) {
        String username = required(request.username(), "Username is required");
        String displayName = required(request.displayName(), "Display name is required");
        String accountType = normalizeAccountType(request.accountType());
        String status = normalizeStatus(request.status());
        validateUnique(username, request.email(), null);

        AccountRecord account = new AccountRecord();
        account.setUsername(username);
        account.setEmail(trimToNull(request.email()));
        account.setPasswordHash(passwordEncoder.encode(required(request.password(), "Password is required")));
        account.setDisplayName(displayName);
        account.setPhone(trimToNull(request.phone()));
        account.setAccountType(accountType);
        account.setStatus(status);
        if (mapper.insert(account) != 1 || account.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create account");
        }
        return findById(account.getId());
    }

    /** Creates the owner portal account required by a newly registered owner. */
    @Transactional
    public Long createOwnerAccount(String phone, String displayName, String email) {
        String username = required(phone, "Owner phone is required to create an account");
        validateUnique(username, email, null);
        AccountRecord account = new AccountRecord();
        account.setUsername(username);
        account.setEmail(trimToNull(email));
        account.setPasswordHash(passwordEncoder.encode(DEFAULT_OWNER_PASSWORD));
        account.setDisplayName(required(displayName, "Owner display name is required"));
        account.setPhone(username);
        account.setAccountType(AuthService.OWNER_ROLE);
        account.setStatus("active");
        if (mapper.insert(account) != 1 || account.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create owner account");
        }
        return account.getId();
    }

    @Transactional
    public AdminAccountResponse update(Long id, AdminAccountUpdateRequest request) {
        AccountRow current = require(id);
        String username = request.username() == null ? current.getUsername() : required(request.username(), "Username is required");
        String displayName = request.displayName() == null ? current.getDisplayName() : required(request.displayName(), "Display name is required");
        String accountType = request.accountType() == null ? current.getAccountType() : normalizeAccountType(request.accountType());
        if (current.getOwnerId() != null && !AuthService.OWNER_ROLE.equals(accountType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An owner account must remain an OWNER account");
        }
        validateUnique(username, request.email(), id);

        AccountRecord account = new AccountRecord();
        account.setId(id);
        account.setUsername(username);
        account.setEmail(request.email() == null ? current.getEmail() : trimToNull(request.email()));
        account.setPasswordHash(request.password() == null || request.password().isBlank()
                ? currentPasswordHash(id)
                : passwordEncoder.encode(request.password()));
        account.setDisplayName(displayName);
        account.setPhone(request.phone() == null ? current.getPhone() : trimToNull(request.phone()));
        account.setAccountType(accountType);
        account.setStatus(request.status() == null ? current.getStatus() : normalizeStatus(request.status()));
        if (mapper.update(account) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account was changed by another request");
        }
        return findById(id);
    }

    @Transactional
    public void delete(Long id) {
        require(id);
        if (mapper.deactivate(id) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
    }

    private AccountRow require(Long id) {
        AccountRow account = mapper.findById(id);
        if (account == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        return account;
    }

    private String currentPasswordHash(Long id) {
        String hash = mapper.findPasswordHash(id);
        if (hash == null || hash.isBlank()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Account password is unavailable");
        return hash;
    }

    private void validateUnique(String username, String email, Long id) {
        if (mapper.countUsername(username, id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        String normalizedEmail = trimToNull(email);
        if (normalizedEmail != null && mapper.countEmail(normalizedEmail, id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
    }

    private AdminAccountResponse toResponse(AccountRow row) {
        return new AdminAccountResponse(row.getId(), row.getUsername(), row.getEmail(), row.getDisplayName(),
                row.getPhone(), row.getAccountType(), row.getStatus(), row.getOwnerId());
    }

    private String normalizeAccountType(String value) {
        String normalized = required(value, "Account type is required").toUpperCase();
        if (!AuthService.ADMIN_ROLE.equals(normalized) && !AuthService.OWNER_ROLE.equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported account type");
        }
        return normalized;
    }

    private String normalizeStatus(String value) {
        String normalized = required(value, "Account status is required").toLowerCase();
        if (!"active".equals(normalized) && !"inactive".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported account status");
        }
        return normalized;
    }

    private String required(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
