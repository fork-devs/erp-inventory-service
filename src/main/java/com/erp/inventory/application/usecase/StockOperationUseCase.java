package com.erp.inventory.application.usecase;

import com.erp.inventory.application.dto.command.RegisterStockMovementCommand;
import com.erp.inventory.application.dto.command.ReleaseStockCommand;
import com.erp.inventory.application.dto.command.ReserveStockCommand;
import com.erp.inventory.application.dto.result.StockBalanceResult;
import com.erp.inventory.application.dto.result.StockMovementResult;

import java.util.UUID;

public interface StockOperationUseCase {
    StockMovementResult registerMovement(RegisterStockMovementCommand command);
    StockMovementResult reserve(ReserveStockCommand command);
    StockMovementResult release(ReleaseStockCommand command);
    StockBalanceResult getBalance(UUID sparePartId, UUID branchId, UUID tenantId);
}
