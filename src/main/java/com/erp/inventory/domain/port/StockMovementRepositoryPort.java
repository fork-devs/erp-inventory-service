package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.StockMovement;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface StockMovementRepositoryPort {
    StockMovement save(StockMovement movement);
    List<StockMovement> findBySparePartAndBranch(SparePartId sparePartId, BranchId branchId, TenantId tenantId);
    List<StockMovement> findBySparePart(SparePartId sparePartId, TenantId tenantId);
    Optional<StockMovement> findByIdempotencyKey(String idempotencyKey, TenantId tenantId);
}
