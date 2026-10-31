package com.erp.inventory.infrastructure.persistence.repository;

import com.erp.inventory.infrastructure.persistence.entity.ProductJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Optional<ProductJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ProductJpaEntity> findByBranchIdAndSkuAndTenantId(UUID branchId, String sku, UUID tenantId);

    List<ProductJpaEntity> findByBranchIdAndTenantId(UUID branchId, UUID tenantId);

    List<ProductJpaEntity> findByBranchIdAndCategoryAndTenantId(UUID branchId, String category, UUID tenantId);

    boolean existsByBranchIdAndSkuAndTenantId(UUID branchId, String sku, UUID tenantId);
}
