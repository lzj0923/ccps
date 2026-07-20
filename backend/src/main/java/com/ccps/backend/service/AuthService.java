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
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.identifier())
                .or()
                .eq(User::getEmail, request.identifier()));

        if (user == null || !"active".equalsIgnoreCase(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    normalizedRequiredRole + " portal access required");
        }
        String activeRole = normalizedRequiredRole != null
                ? normalizedRequiredRole
                : defaultRole(roleCodes);

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return new LoginResponse(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(),
                user.getStatus(), activeRole, List.copyOf(roleCodes));
    }

    private String normalizeAccountType(String accountType) {
        String normalized = accountType == null ? "" : accountType.toUpperCase(Locale.ROOT);
        if (ADMIN_ROLE.equals(normalized) || OWNER_ROLE.equals(normalized)) return normalized;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unsupported account type");
    }

    private String defaultRole(Set<String> roles) {
        if (roles.contains(ADMIN_ROLE)) return ADMIN_ROLE;
        if (roles.contains(OWNER_ROLE)) return OWNER_ROLE;
        return "USER";
    }
}
