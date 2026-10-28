package com.erp.inventory.domain.exception;

/**
 * Base domain exception providing a stable, machine-readable error code per family.
 *
 * <p>The error code is what clients consume: messages can be rewritten without breaking contracts,
 * whereas code changes constitute breaking changes.
 */
public class DomainException extends RuntimeException {

    private final String code;

    public DomainException(String message) {
        this("INVENTORY_ERROR", message);
    }

    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
