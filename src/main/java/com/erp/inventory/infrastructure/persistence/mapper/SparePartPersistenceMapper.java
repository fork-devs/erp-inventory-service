package com.erp.inventory.infrastructure.persistence.mapper;

import com.erp.inventory.domain.model.SparePart;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.entity.SparePartJpaEntity;

public class SparePartPersistenceMapper {
    private SparePartPersistenceMapper() {}

    public static SparePartJpaEntity toEntity(SparePart p) {
        return new SparePartJpaEntity(
                p.getId().getValue(), p.getTenantId().getValue(),
                p.getSku().getValue(), p.getDescription(), p.getMinStock(), p.getCreatedAt()
        );
    }

    public static SparePart toDomain(SparePartJpaEntity e) {
        return new SparePart(
                new SparePartId(e.getId()), new TenantId(e.getTenantId()),
                Sku.of(e.getSku()), e.getDescription(), e.getMinStock(), e.getCreatedAt()
        );
    }
}
