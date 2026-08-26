package com.ccps.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminRoutePermissionPolicyTest {
    @Test
    void smartDashboardIsReadableByEveryBackofficeRole() {
        assertThat(AdminRoutePermissionPolicy.requiredAny("GET", "/api/admin/dashboard"))
                .containsExactly(AdminPermissionCodes.BACKOFFICE_VIEW);
    }

    @Test
    void financeConfirmationCannotBePerformedWithBusinessPermission() {
        assertThat(AdminRoutePermissionPolicy.requiredAny("POST", "/api/admin/finance/reviews/9/confirm"))
                .containsExactly(AdminPermissionCodes.FINANCE_MANAGE);
        assertThat(AdminRoutePermissionPolicy.requiredAny("POST", "/api/admin/tenancy/rent-invoices/batch-confirm"))
                .containsExactly(AdminPermissionCodes.FINANCE_MANAGE);
    }

    @Test
    void accountManagementIsRestrictedToSystemManagers() {
        assertThat(AdminRoutePermissionPolicy.requiredAny("POST", "/api/admin/accounts"))
                .containsExactly(AdminPermissionCodes.SYSTEM_MANAGE);
        assertThat(AdminRoutePermissionPolicy.requiredAny("GET", "/api/admin/accounts"))
                .containsExactly(AdminPermissionCodes.SYSTEM_MANAGE);
    }

    @Test
    void serviceAndBusinessCanBothHandleOperationalWork() {
        assertThat(AdminRoutePermissionPolicy.requiredAny("POST", "/api/admin/expenses/maintenance"))
                .containsExactlyInAnyOrder(AdminPermissionCodes.OPERATIONS_MANAGE,
                        AdminPermissionCodes.BUSINESS_MANAGE);
    }
}
