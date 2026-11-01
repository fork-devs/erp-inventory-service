package com.erp.inventory.domain.model;

public enum MovementType {
    IN,
    OUT,
    RESERVE,
    RELEASE,
    /** Consumes reserved stock: converts the reservation into an actual outgoing movement. */
    COMMIT
}
