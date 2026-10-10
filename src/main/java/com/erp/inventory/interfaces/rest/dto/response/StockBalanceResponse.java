package com.erp.inventory.interfaces.rest.dto.response;

import java.util.UUID;

public record StockBalanceResponse(
        UUID sparePartId,
        UUID branchId,
        int totalIn,
        int totalOut,
        int totalReserved,
        int availableStock
) {}
