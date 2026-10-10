package com.erp.inventory.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record StockMovementResult(
        UUID id,
        UUID tenantId,
        UUID sparePartId,
        UUID branchId,
        String movementType,
        int quantity,
        UUID referenceId,
        String idempotencyKey,
        Instant createdAt
) {}
