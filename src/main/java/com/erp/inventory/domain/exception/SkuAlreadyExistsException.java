package com.erp.inventory.domain.exception;

public class SkuAlreadyExistsException extends DomainException {
    public SkuAlreadyExistsException(String sku) {
        super("Ya existe un repuesto con el SKU: " + sku);
    }
}
