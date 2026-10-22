package com.erp.inventory.infrastructure.persistence.mapper;

import com.erp.inventory.domain.model.Branch;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.entity.BranchJpaEntity;

public class BranchPersistenceMapper {
    private BranchPersistenceMapper() {}

    public static BranchJpaEntity toEntity(Branch b) {
        return new BranchJpaEntity(
                b.getId().getValue(), b.getTenantId().getValue(),
                b.getName(), b.getAddress(), b.getCreatedAt()
        );
    }

    public static Branch toDomain(BranchJpaEntity e) {
        return new Branch(
                new BranchId(e.getId()), new TenantId(e.getTenantId()),
                e.getName(), e.getAddress(), e.getCreatedAt()
        );
    }
}
