package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReserveStockRequest(
        @NotNull(message = "{validation.inventory.stock.spare-part-id.not-null}")
        UUID sparePartId,
        @NotNull(message = "{validation.inventory.stock.branch-id.not-null}")
        UUID branchId,
        @Min(value = 1, message = "{validation.inventory.stock.quantity.min}")
        int quantity,
        UUID workOrderId,
        String idempotencyKey
) {}
