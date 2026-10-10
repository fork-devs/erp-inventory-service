package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveStockRequest(
        @NotNull UUID sparePartId,
        @NotNull UUID branchId,
        @Min(1) int quantity,
        UUID workOrderId,
        String idempotencyKey
) {}
