package com.erp.inventory.domain.exception;

import java.util.UUID;

public class ActiveStockConflictException extends DomainException {
    public ActiveStockConflictException(UUID productId, int currentStock) {
        super("PRODUCT_HAS_STOCK", "Cannot delete product " + productId + " because it has active stock: " + currentStock);
    }
}
