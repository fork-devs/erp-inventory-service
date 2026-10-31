package com.erp.inventory.infrastructure.persistence.repository;

import com.erp.inventory.infrastructure.persistence.entity.ProductBatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataProductBatchRepository extends JpaRepository<ProductBatchJpaEntity, UUID> {

    List<ProductBatchJpaEntity> findByProductIdAndTenantId(UUID productId, UUID tenantId);

    Optional<ProductBatchJpaEntity> findByProductIdAndBatchNumberAndTenantId(UUID productId, String batchNumber, UUID tenantId);
}
