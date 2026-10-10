package com.erp.inventory.domain.exception;

public class BranchNotFoundException extends DomainException {
    public BranchNotFoundException(String id) {
        super("Sucursal no encontrada con ID: " + id);
    }
}
