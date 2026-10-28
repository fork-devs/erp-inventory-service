package com.erp.inventory.interfaces.rest.support;

import com.erp.inventory.domain.exception.TenantContextMissingException;
import com.erp.inventory.infrastructure.persistence.rls.TenantContextHolder;

import java.util.UUID;

/**
 * Helper to retrieve the current request's workshop tenant ID.
 *
 * <p>The context is established by the authentication filter from the verified token.
 * The {@code X-Tenant-ID} header is only cross-checked; it never decides tenant identity on its own.
 */
public final class CurrentTenant {

    private CurrentTenant() {
    }

    public static UUID id() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException(
                    "La peticion no tiene contexto de taller: el token verificado no lo declara.");
        }
        return tenantId;
    }
}
