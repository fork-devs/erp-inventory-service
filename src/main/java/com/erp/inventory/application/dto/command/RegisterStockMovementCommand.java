package com.erp.inventory.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Alta de un movimiento manual de stock.
 *
 * <p>La ENTRADA ({@code IN}) lleva el coste unitario real con el que se
 * compro: es el que abre el lote y el que valora el stock. Las demas
 * operaciones no lo llevan: su valor sale de los lotes que consumen.
 */
public record RegisterStockMovementCommand(
        UUID tenantId,
        UUID sparePartId,
        UUID branchId,
        String movementType,
        int quantity,
        BigDecimal unitCost,
        UUID referenceId,
        String idempotencyKey
) {
}
