package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Alta de un movimiento manual de stock.
 *
 * <p>La ENTRADA ({@code IN}) lleva el coste unitario real con el que se
 * compro: es el que abre el lote y el que valora el stock. Las demas
 * operaciones no lo llevan: su valor sale de los lotes que consumen.
 */
public record RegisterStockMovementRequest(
        @NotNull UUID sparePartId,
        @NotNull UUID branchId,
        @NotBlank String movementType,
        @Min(1) int quantity,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitCost,
        UUID referenceId,
        String idempotencyKey
) {
}
