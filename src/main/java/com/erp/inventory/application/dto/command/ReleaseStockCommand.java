package com.erp.inventory.application.dto.command;

import java.util.UUID;

public record ReleaseStockCommand(
        UUID tenantId,
        UUID sparePartId,
        UUID branchId,
        int quantity,
        UUID workOrderId,
        String idempotencyKey
) {}
