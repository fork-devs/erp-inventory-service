package com.erp.inventory.application.usecase;

import com.erp.inventory.application.dto.command.RegisterStockMovementCommand;
import com.erp.inventory.application.dto.command.ReleaseStockCommand;
import com.erp.inventory.application.dto.command.ReserveStockCommand;
import com.erp.inventory.application.dto.result.StockBalanceResult;
import com.erp.inventory.application.dto.result.StockMovementResult;

import java.util.UUID;

/**
 * Operaciones de stock.
 *
 * <p>La saga de la orden de trabajo tiene las TRES operaciones: reservar,
 * confirmar y liberar. Sin la confirmacion, una reserva aprobada nunca se
 * convertia en una salida real y el stock quedaba reservado para siempre.
 */
public interface StockOperationUseCase {

    StockMovementResult registerMovement(RegisterStockMovementCommand command);

    StockMovementResult reserve(ReserveStockCommand command);

    /** Confirma una reserva: el stock reservado se consume. */
    StockMovementResult commit(ReserveStockCommand command);

    StockMovementResult release(ReleaseStockCommand command);

    StockBalanceResult getBalance(UUID sparePartId, UUID branchId, UUID tenantId);
}
