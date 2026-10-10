package com.erp.inventory.infrastructure.persistence.mapper;

import com.erp.inventory.domain.model.MovementType;
import com.erp.inventory.domain.model.StockMovement;
import com.erp.inventory.domain.valueobject.*;
import com.erp.inventory.infrastructure.persistence.entity.StockMovementJpaEntity;

public class StockMovementPersistenceMapper {
    private StockMovementPersistenceMapper() {}

    public static StockMovementJpaEntity toEntity(StockMovement m) {
        return new StockMovementJpaEntity(
                m.getId().getValue(), m.getTenantId().getValue(),
                m.getSparePartId().getValue(), m.getBranchId().getValue(),
                m.getMovementType().name(), m.getQuantity().getValue(),
                m.getReferenceId(), m.getIdempotencyKey() != null ? m.getIdempotencyKey().getValue() : null,
                m.getCreatedAt()
        );
    }

    public static StockMovement toDomain(StockMovementJpaEntity e) {
        return new StockMovement(
                new StockMovementId(e.getId()), new TenantId(e.getTenantId()),
                new SparePartId(e.getSparePartId()), new BranchId(e.getBranchId()),
                MovementType.valueOf(e.getMovementType()), Quantity.of(e.getQuantity()),
                e.getReferenceId(), e.getIdempotencyKey() != null ? IdempotencyKey.of(e.getIdempotencyKey()) : null,
                e.getCreatedAt()
        );
    }
}
