package com.erp.inventory.application.usecase;

import com.erp.inventory.application.dto.command.RegisterStockMovementCommand;
import com.erp.inventory.application.dto.command.ReleaseStockCommand;
import com.erp.inventory.application.dto.command.ReserveStockCommand;
import com.erp.inventory.application.dto.result.StockBalanceResult;
import com.erp.inventory.application.dto.result.StockMovementResult;

import java.util.UUID;

/**
 * Use case defining core stock operations.
 *
 * <p>The work order saga encompasses THREE operations: reserve, commit, and release.
 * Without the commit operation, an approved reservation never transitioned into an actual
 * consumption, leaving stock indefinitely reserved.
 */
public interface StockOperationUseCase {

    StockMovementResult registerMovement(RegisterStockMovementCommand command);

    StockMovementResult reserve(ReserveStockCommand command);

    /** Commits a reservation: permanently consumes reserved stock. */
    StockMovementResult commit(ReserveStockCommand command);

    StockMovementResult release(ReleaseStockCommand command);

    StockBalanceResult getBalance(UUID sparePartId, UUID branchId, UUID tenantId);
}
