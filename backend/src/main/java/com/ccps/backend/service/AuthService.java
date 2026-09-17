package com.ccps.backend.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccps.backend.dto.LoginRequest;
import com.ccps.backend.dto.LoginResponse;
import com.ccps.backend.mapper.UserMapper;
import com.ccps.backend.model.User;

@Service
public class AuthService {
    public static final String ADMIN_ROLE = "ADMIN";
    public static final String OWNER_ROLE = "OWNER";

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public LoginResponse login(LoginRequest request) {
        return login(request, null);
    }

    public LoginResponse login(LoginRequest request, String requiredRole) {
        List<String> identifiers = loginIdentifiers(request.identifier());
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .in(User::getUsername, identifiers)
                .or()
                .in(User::getEmail, identifiers)
                .or()
                .in(User::getPhone, identifiers));

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (!"active".equalsIgnoreCase(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        Set<String> roleCodes = new LinkedHashSet<>();
        List<String> storedRoles = userMapper.findRoleCodes(user.getId());
        if (storedRoles != null) {
            storedRoles.stream()
                    .filter(role -> role != null && !role.isBlank())
                    .map(role -> role.toUpperCase(Locale.ROOT))
                    .forEach(roleCodes::add);
        }
        String normalizedRequiredRole = requiredRole == null
                ? null
                : requiredRole.toUpperCase(Locale.ROOT);
        String accountType = normalizeAccountType(user.getAccountType());
        roleCodes.remove(ADMIN_ROLE);
        roleCodes.remove(OWNER_ROLE);
        roleCodes.add(accountType);
        if (normalizedRequiredRole != null && !normalizedRequiredRole.equals(accountType)) {
            String portal = ADMIN_ROLE.equals(accountType) ? "administrator" : "owner";
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This account can only sign in through the " + portal + " portal");
        }
        String activeRole = normalizedRequiredRole != null
                ? normalizedRequiredRole
                : defaultRole(roleCodes);
        List<String> permissionCodes = userMapper.findPermissionCodes(user.getId());

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return new LoginResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(),
                user.getStatus(), activeRole, List.copyOf(roleCodes),
                permissionCodes == null ? List.of() : permissionCodes.stream()
                        .filter(code -> code != null && !code.isBlank())
                        .map(code -> code.toUpperCase(Locale.ROOT)).distinct().toList());
    }

    private List<String> loginIdentifiers(String identifier) {
        String raw = identifier == null ? "" : identifier.trim();
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(raw);
        if (!raw.matches("[+\\d\\s().-]+")) return List.copyOf(candidates);

        String digits = raw.replaceAll("\\D", "");
        if (digits.isBlank()) return List.copyOf(candidates);
        candidates.add(digits);
        candidates.add("+" + digits);
        addNationalPhoneCandidates(candidates, digits, "86", 11, false);
        addNationalPhoneCandidates(candidates, digits, "60", 9, true);
        addNationalPhoneCandidates(candidates, digits, "66", 9, true);
        return List.copyOf(candidates);
    }

    private void addNationalPhoneCandidates(Set<String> candidates, String digits,
            String countryCode, int minimumLength, boolean includeLeadingZero) {
        if (!digits.startsWith(countryCode)) return;
        String national = digits.substring(countryCode.length());
        if (national.length() < minimumLength) return;
        candidates.add(national);
        if (includeLeadingZero && !national.startsWith("0")) candidates.add("0" + national);
    }

    private String normalizeAccountType(String accountType) {
        String normalized = accountType == null ? "" : accountType.toUpperCase(Locale.ROOT);
        if (ADMIN_ROLE.equals(normalized) || OWNER_ROLE.equals(normalized)) return normalized;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account type is not configured correctly");
    }

    private String defaultRole(Set<String> roles) {
        if (roles.contains(ADMIN_ROLE)) return ADMIN_ROLE;
        if (roles.contains(OWNER_ROLE)) return OWNER_ROLE;
        return "USER";
    }
}
