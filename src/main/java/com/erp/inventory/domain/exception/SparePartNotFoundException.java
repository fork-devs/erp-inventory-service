package com.erp.inventory.domain.exception;

public class SparePartNotFoundException extends DomainException {
    public SparePartNotFoundException(String id) {
        super("Repuesto no encontrado con ID: " + id);
    }
}
