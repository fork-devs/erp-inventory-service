package com.erp.inventory.application.dto.result;

import java.util.UUID;

public record StockBalanceResult(
        UUID sparePartId,
        UUID branchId,
        int totalIn,
        int totalOut,
        int totalReserved,
        int availableStock
) {}
