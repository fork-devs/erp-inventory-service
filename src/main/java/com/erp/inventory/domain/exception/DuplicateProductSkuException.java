package com.erp.inventory.domain.exception;

public class DuplicateProductSkuException extends DomainException {
    public DuplicateProductSkuException(String sku) {
        super("VALIDATION_ERROR", "Product with SKU already exists in this branch: " + sku);
    }
}
