package com.waiveliability.security;

import java.util.UUID;

/**
 * ThreadLocal store for the current tenant, populated by JwtFilter on every authenticated request.
 * All service methods use TenantContext.current() — never accept tenantId as a caller parameter.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static UUID current() {
        return TENANT.get();
    }

    public static void set(UUID tenantId) {
        TENANT.set(tenantId);
    }

    public static void clear() {
        TENANT.remove();
    }
}
