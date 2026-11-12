package com.yms.yms.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigPermissionTest {

    @Test
    void writePermissionsAlsoAllowReadOperations() {
        for (String module : Set.of("USER", "PARTNER", "CONTRACT", "SERVICE", "YARD",
                "TRACKING", "FINANCE", "REPORT", "GOODS", "CONFIRMATION")) {
            assertTrue(SecurityConfig.hasRequiredPermission(Set.of(module + "_WRITE"), module + "_READ"),
                    module + "_WRITE should allow " + module + "_READ");
            assertFalse(SecurityConfig.hasRequiredPermission(Set.of(module + "_READ"), module + "_WRITE"),
                    module + "_READ must not allow " + module + "_WRITE");
        }
    }

    @Test
    void financePermissionsAllowPartnerLookupsRequiredByFinanceScreens() {
        assertTrue(SecurityConfig.hasRequiredPermission(Set.of("FINANCE_READ"), "PARTNER_READ"));
        assertTrue(SecurityConfig.hasRequiredPermission(Set.of("FINANCE_WRITE"), "PARTNER_READ"));
        assertFalse(SecurityConfig.hasRequiredPermission(Set.of("FINANCE_READ"), "PARTNER_WRITE"));
    }

    @Test
    void unmappedRoutesAreDeniedByDefault() {
        assertFalse(SecurityConfig.hasRequiredPermission(Set.of("FINANCE_WRITE"), null));
    }

    @Test
    void everyModuleRouteMapsToTheExpectedPermission() {
        assertPermission("GET", "/api/users", "USER_READ");
        assertPermission("POST", "/api/users", "USER_WRITE");
        assertPermission("GET", "/api/roles", "USER_READ");
        assertPermission("GET", "/api/partners", "PARTNER_READ");
        assertPermission("POST", "/api/partners", "PARTNER_WRITE");
        assertPermission("GET", "/api/contracts", "CONTRACT_READ");
        assertPermission("PUT", "/api/contracts/1", "CONTRACT_WRITE");
        assertPermission("GET", "/api/services", "SERVICE_READ");
        assertPermission("POST", "/api/services", "SERVICE_WRITE");
        assertPermission("GET", "/api/service-orders", "SERVICE_DISPATCH");
        assertPermission("POST", "/api/vehicles", "VEHICLE_OPERATE");
        assertPermission("GET", "/api/yard-logs", "GATE_OPERATE");
        assertPermission("GET", "/api/yards", "YARD_READ");
        assertPermission("PUT", "/api/yards/1", "YARD_WRITE");
        assertPermission("GET", "/api/trips", "TRACKING_READ");
        assertPermission("POST", "/api/tracking", "TRACKING_WRITE");
        assertPermission("GET", "/api/finance/debts", "FINANCE_READ");
        assertPermission("POST", "/api/finance/invoices", "FINANCE_WRITE");
        assertPermission("GET", "/api/finance/interest-configs", "FINANCE_WRITE");
        assertPermission("GET", "/api/reports/monthly", "REPORT_READ");
        assertPermission("GET", "/api/reports/schedules", "REPORT_WRITE");
        assertPermission("GET", "/api/goods", "GOODS_READ");
        assertPermission("POST", "/api/confirmations", "CONFIRMATION_WRITE");
    }

    private static void assertPermission(String method, String path, String expected) {
        assertEquals(expected, SecurityConfig.requiredPermission(method, path), path);
    }
}
