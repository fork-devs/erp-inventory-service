package com.erp.inventory.domain.exception;

public class InsufficientStockException extends DomainException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
