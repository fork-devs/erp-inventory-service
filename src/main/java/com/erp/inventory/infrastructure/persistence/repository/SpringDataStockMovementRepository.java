package com.erp.inventory.infrastructure.persistence.repository;

import com.erp.inventory.infrastructure.persistence.entity.StockMovementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataStockMovementRepository extends JpaRepository<StockMovementJpaEntity, UUID> {
    List<StockMovementJpaEntity> findBySparePartIdAndBranchIdAndTenantId(UUID sparePartId, UUID branchId, UUID tenantId);
    List<StockMovementJpaEntity> findBySparePartIdAndTenantId(UUID sparePartId, UUID tenantId);
    Optional<StockMovementJpaEntity> findByIdempotencyKeyAndTenantId(String idempotencyKey, UUID tenantId);
}
