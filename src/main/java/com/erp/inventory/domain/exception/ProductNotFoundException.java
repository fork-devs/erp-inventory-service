package com.erp.inventory.domain.exception;

import java.util.UUID;

public class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(UUID id) {
        super("PRODUCT_NOT_FOUND", "Product not found with id: " + id);
    }
}
