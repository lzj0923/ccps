package com.ccps.backend.security;

import java.util.Set;

public final class AdminPermissionCodes {
    public static final String BACKOFFICE_VIEW = "BACKOFFICE_VIEW";
    public static final String BUSINESS_MANAGE = "BUSINESS_MANAGE";
    public static final String FINANCE_MANAGE = "FINANCE_MANAGE";
    public static final String OPERATIONS_MANAGE = "OPERATIONS_MANAGE";
    public static final String SYSTEM_MANAGE = "SYSTEM_MANAGE";
    public static final String REPORT_MANAGE = "REPORT_MANAGE";

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String FINANCE = "FINANCE";
    public static final String BUSINESS = "BUSINESS";
    public static final String CUSTOMER_SERVICE = "CUSTOMER_SERVICE";
    public static final String ADMINISTRATION = "ADMINISTRATION";

    public static final Set<String> STAFF_ROLES = Set.of(
            SUPER_ADMIN, FINANCE, BUSINESS, CUSTOMER_SERVICE, ADMINISTRATION);

    private AdminPermissionCodes() {
    }
}
