package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.StockLot;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.util.List;

/**
 * Repository port for managing stock lots of a spare part at a branch.
 *
 * <p>Consumption follows FIFO: lots are processed from oldest to newest.
 * To prevent over-reservation across concurrent requests, lots are **pessimistically locked**
 * before calculating available stock: subsequent requests wait for the lock and read updated stock.
 */
public interface StockLotRepositoryPort {

    /**
     * Retrieves stock lots for a spare part at a branch, **pessimistically locked** and ordered by entry date.
     *
     * @param tenantId workshop tenant identifier
     * @param sparePartId spare part identifier
     * @param branchId branch identifier
     * @return list of locked stock lots in FIFO order
     */
    List<StockLot> bloquearLotes(TenantId tenantId, SparePartId sparePartId, BranchId branchId);

    StockLot guardar(StockLot lot);

    List<StockLot> listar(TenantId tenantId, SparePartId sparePartId, BranchId branchId);
}
