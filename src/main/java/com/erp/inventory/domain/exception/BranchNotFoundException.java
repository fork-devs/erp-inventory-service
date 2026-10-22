package com.erp.inventory.domain.exception;

public class BranchNotFoundException extends DomainException {
    public BranchNotFoundException(String id) {
        super("INVENTORY_BRANCH_NOT_FOUND", "Sucursal no encontrada con ID: " + id);
    }
}
