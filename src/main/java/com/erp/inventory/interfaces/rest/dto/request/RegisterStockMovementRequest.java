package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RegisterStockMovementRequest(
        @NotNull UUID sparePartId,
        @NotNull UUID branchId,
        @NotBlank String movementType,
        @Min(1) int quantity,
        UUID referenceId,
        String idempotencyKey
) {}
