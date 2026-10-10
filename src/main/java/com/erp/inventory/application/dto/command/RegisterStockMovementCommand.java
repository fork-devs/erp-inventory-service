package com.erp.inventory.application.dto.command;

import java.util.UUID;

public record RegisterStockMovementCommand(
        UUID tenantId,
        UUID sparePartId,
        UUID branchId,
        String movementType,
        int quantity,
        UUID referenceId,
        String idempotencyKey
) {}
