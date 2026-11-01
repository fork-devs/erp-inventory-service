package com.erp.inventory.application.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stock balance for a spare part at a branch.
 *
 * <p>The total valuation is derived from the remaining LOTS, not from summing
 * movements: the balance indicates how many units are available, and the valuation
 * indicates their total monetary value.
 */
public record StockBalanceResult(
        UUID sparePartId,
        UUID branchId,
        int totalIn,
        int totalOut,
        int totalReserved,
        int availableStock,
        BigDecimal availableValue,
        BigDecimal reservedValue
) {
}
