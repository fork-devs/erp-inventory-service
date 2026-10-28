package com.erp.inventory.domain.exception;

/**
 * Thrown when an operation requiring tenant isolation executes without
 * an established tenant context.
 *
 * <p>Tenant isolation fails closed: operations without tenant context are rejected
 * rather than executed without filtering.
 */
public class TenantContextMissingException extends DomainException {

    public TenantContextMissingException() {
        super("INVENTORY_TENANT_CONTEXT_MISSING",
                "La operacion requiere un contexto de taller establecido y no lo tiene.");
    }

    public TenantContextMissingException(String message) {
        super("INVENTORY_TENANT_CONTEXT_MISSING", message);
    }
}
