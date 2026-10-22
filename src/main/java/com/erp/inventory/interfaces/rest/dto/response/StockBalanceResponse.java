package com.erp.inventory.interfaces.rest.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saldo de un repuesto en una sucursal, con su valor.
 */
public record StockBalanceResponse(
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
