package com.erp.inventory.interfaces.rest.support;

import com.erp.inventory.infrastructure.persistence.rls.TenantContextHolder;

import java.util.UUID;

/**
 * Scope guard for the workshop tenant context of an operation.
 *
 * <p>Sets the tenant context and restores previous state upon closing (via {@link AutoCloseable}),
 * ensuring the context does not leak into subsequent operations on the same pooled thread.
 */
public final class TenantContextScope implements AutoCloseable {

    private final UUID previousTenantId;
    private boolean closed;

    private TenantContextScope(UUID previousTenantId) {
        this.previousTenantId = previousTenantId;
    }

    public static TenantContextScope forTenant(UUID tenantId) {
        UUID previous = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        return new TenantContextScope(previous);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (previousTenantId == null) {
            TenantContextHolder.clear();
        } else {
            TenantContextHolder.setTenantId(previousTenantId);
        }
    }
}
