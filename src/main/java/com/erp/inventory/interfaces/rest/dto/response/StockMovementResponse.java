package com.erp.inventory.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record StockMovementResponse(
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
