package com.ccps.backend.service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ccps.backend.mapper.UserMapper;
import com.ccps.backend.security.AdminPermissionCodes;
import com.ccps.backend.security.AdminRoutePermissionPolicy;

@Service
public class AdminPermissionService {
    private final UserMapper userMapper;

    public AdminPermissionService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public boolean isAllowed(Long userId, String method, String requestPath) {
        Set<String> roles = normalized(userMapper.findRoleCodes(userId));
        Set<String> staffRoles = new HashSet<>(roles);
        staffRoles.retainAll(AdminPermissionCodes.STAFF_ROLES);

        // Backwards compatibility before the RBAC migration is applied.
        if (staffRoles.isEmpty() || staffRoles.contains(AdminPermissionCodes.SUPER_ADMIN)) return true;

        Set<String> required = AdminRoutePermissionPolicy.requiredAny(method, requestPath);
        if (required.isEmpty()) return true;
        Set<String> permissions = normalized(userMapper.findPermissionCodes(userId));
        return required.stream().anyMatch(permissions::contains);
    }

    public boolean isSuperAdmin(Long userId) {
        Set<String> roles = normalized(userMapper.findRoleCodes(userId));
        Set<String> staffRoles = new HashSet<>(roles);
        staffRoles.retainAll(AdminPermissionCodes.STAFF_ROLES);
        // Keep the original administrator usable until the RBAC migration has run.
        return staffRoles.isEmpty() || staffRoles.contains(AdminPermissionCodes.SUPER_ADMIN);
    }

    private Set<String> normalized(List<String> values) {
        Set<String> result = new HashSet<>();
        if (values == null) return result;
        values.stream().filter(value -> value != null && !value.isBlank())
                .map(value -> value.toUpperCase(Locale.ROOT)).forEach(result::add);
        return result;
    }
}
