package com.erp.inventory.application.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Saldo de un repuesto en una sucursal.
 *
 * <p>El valor viene de los LOTES que quedan, no de la suma de los
 * movimientos: el saldo dice cuantas unidades hay, y el valor dice cuanto
 * valen.
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
