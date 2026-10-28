package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO to register a manual stock movement.
 *
 * <p>An incoming movement ({@code IN}) carries the actual unit cost at which it
 * was purchased: this opens the lot and values the stock. Other operations
 * do not carry unit cost: their value is derived from the lots they consume.
 */
public record RegisterStockMovementRequest(
        @NotNull(message = "{validation.inventory.movement.spare-part-id.not-null}")
        UUID sparePartId,
        @NotNull(message = "{validation.inventory.movement.branch-id.not-null}")
        UUID branchId,
        @NotBlank(message = "{validation.inventory.movement.movement-type.not-blank}")
        String movementType,
        @Min(value = 1, message = "{validation.inventory.movement.quantity.min}")
        int quantity,
        @DecimalMin(value = "0.0", inclusive = true, message = "{validation.inventory.movement.unit-cost.min}")
        BigDecimal unitCost,
        UUID referenceId,
        String idempotencyKey
) {
}
