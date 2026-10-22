package com.erp.inventory.domain.exception;

/**
 * Se lanza cuando una operacion que requiere aislamiento por taller se
 * ejecuta sin contexto de taller establecido.
 *
 * <p>El aislamiento no se relaja de forma silenciosa: una operacion sin
 * contexto no se ejecuta sin filtro, se rechaza.
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
