package com.erp.inventory.domain.exception;

public class InvalidBatchException extends DomainException {
    public InvalidBatchException(String message) {
        super("INVALID_BATCH", message);
    }
}
