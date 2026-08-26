package com.ccps.backend.security;

import java.util.Set;

public final class AdminRoutePermissionPolicy {
    private AdminRoutePermissionPolicy() {
    }

    public static Set<String> requiredAny(String method, String requestPath) {
        String verb = method == null ? "GET" : method.toUpperCase();
        String path = requestPath == null ? "" : requestPath;
        if ("OPTIONS".equals(verb)) return Set.of();

        if (isRead(verb)) {
            if (startsWithAny(path, "/api/admin/accounts", "/api/admin/audit",
                    "/api/admin/system/", "/api/admin/sync")) {
                return Set.of(AdminPermissionCodes.SYSTEM_MANAGE);
            }
            return Set.of(AdminPermissionCodes.BACKOFFICE_VIEW);
        }

        if (startsWithAny(path, "/api/admin/accounts", "/api/admin/system/", "/api/admin/sync")) {
            return Set.of(AdminPermissionCodes.SYSTEM_MANAGE);
        }
        if (path.startsWith("/api/admin/contract-templates/")
                && (path.endsWith("/template") || path.endsWith("/layout"))) {
            return Set.of(AdminPermissionCodes.SYSTEM_MANAGE);
        }
        if (path.startsWith("/api/admin/reports")) {
            return Set.of(AdminPermissionCodes.REPORT_MANAGE);
        }
        if (isFinanceMutation(path)) {
            return Set.of(AdminPermissionCodes.FINANCE_MANAGE);
        }
        if (isOperationsMutation(path)) {
            return Set.of(AdminPermissionCodes.OPERATIONS_MANAGE, AdminPermissionCodes.BUSINESS_MANAGE);
        }
        return Set.of(AdminPermissionCodes.BUSINESS_MANAGE);
    }

    private static boolean isFinanceMutation(String path) {
        return startsWithAny(path, "/api/admin/finance", "/api/admin/reserve")
                || path.matches(".*/api/admin/tenancy/rent-invoices/(?:batch-confirm|[^/]+/confirm)$")
                || path.contains("/deposit-transactions")
                || path.matches(".*/api/admin/tenancy/leases/[^/]+/payments/[^/]+$")
                || path.matches(".*/api/admin/buildings/installments/[^/]+$");
    }

    private static boolean isOperationsMutation(String path) {
        return startsWithAny(path, "/api/admin/reminders", "/api/admin/expenses", "/api/admin/vendors")
                || path.contains("/important-messages")
                || path.contains("/maintenance-records")
                || path.contains("/repair-reports")
                || path.contains("/operations/")
                || path.contains("/rent-payments/") && path.endsWith("/proof")
                || path.contains("/rent-invoices/") && path.endsWith("/reminders")
                || path.startsWith("/api/admin/e-signatures");
    }

    private static boolean startsWithAny(String path, String... prefixes) {
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private static boolean isRead(String method) {
        return "GET".equals(method) || "HEAD".equals(method);
    }
}
