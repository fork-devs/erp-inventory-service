package com.erp.inventory.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to register a manual stock movement.
 *
 * <p>An incoming movement ({@code IN}) carries the actual unit cost at which it
 * was purchased: this opens the lot and values the stock. Other operations
 * do not carry unit cost: their value is derived from the lots they consume.
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
