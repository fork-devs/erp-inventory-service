package com.erp.inventory.infrastructure.persistence.rls;

import java.util.UUID;

/**
 * Thread-local holder for the active request's workshop tenant context.
 *
 * <p>Populated strictly from the authenticated principal and cleared upon request completion.
 * The tenant context is decided by the verified token claim: the {@code X-Tenant-ID} header
 * is merely cross-checked against it, never treated as authoritative.
 *
 * <p>No bypass mechanisms exist to circumvent tenant isolation.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
