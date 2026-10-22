package com.erp.inventory.domain.exception;

public class SparePartNotFoundException extends DomainException {
    public SparePartNotFoundException(String id) {
        super("INVENTORY_PART_NOT_FOUND", "Repuesto no encontrado con ID: " + id);
    }
}
