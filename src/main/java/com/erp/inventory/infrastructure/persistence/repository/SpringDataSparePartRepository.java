package com.erp.inventory.infrastructure.persistence.repository;

import com.erp.inventory.infrastructure.persistence.entity.SparePartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataSparePartRepository extends JpaRepository<SparePartJpaEntity, UUID> {
    List<SparePartJpaEntity> findAllByTenantId(UUID tenantId);
    Optional<SparePartJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsBySkuAndTenantId(String sku, UUID tenantId);
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
