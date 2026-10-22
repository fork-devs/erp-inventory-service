package com.erp.inventory.domain.model;

public enum MovementType {
    IN,
    OUT,
    RESERVE,
    RELEASE,
    /** Consume el stock reservado: la reserva se convierte en salida real. */
    COMMIT
}
