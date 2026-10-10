package com.erp.inventory.infrastructure.persistence.repository;

import com.erp.inventory.infrastructure.persistence.entity.BranchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataBranchRepository extends JpaRepository<BranchJpaEntity, UUID> {
    List<BranchJpaEntity> findAllByTenantId(UUID tenantId);
    Optional<BranchJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
