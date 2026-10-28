package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.StockMovement;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface StockMovementRepositoryPort {

    StockMovement save(StockMovement movement);

    /**
     * Claims the idempotency key by inserting the movement.
     *
     * <p>The insertion itself constitutes the claim: the unique constraint on
     * {@code (tenant_id, idempotency_key)} arbitrates between concurrent requests
     * with the same key. A check-then-insert approach creates a race window where
     * concurrent requests both pass the check and attempt to insert duplicates.
     *
     * @param movement movement to register
     * @return empty if newly inserted; the existing movement if the key was already claimed.
     */
    Optional<StockMovement> claim(StockMovement movement);

    List<StockMovement> findBySparePartAndBranch(SparePartId sparePartId, BranchId branchId, TenantId tenantId);

    List<StockMovement> findBySparePart(SparePartId sparePartId, TenantId tenantId);

    Optional<StockMovement> findByIdempotencyKey(String idempotencyKey, TenantId tenantId);
}
