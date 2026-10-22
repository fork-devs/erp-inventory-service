package com.erp.inventory.infrastructure.persistence.mapper;

import com.erp.inventory.domain.model.StockOutboxMessage;
import com.erp.inventory.domain.model.StockOutboxStatus;
import com.erp.inventory.infrastructure.persistence.entity.StockOutboxJpaEntity;

public final class StockOutboxPersistenceMapper {

    private StockOutboxPersistenceMapper() {}

    public static StockOutboxMessage toDomain(StockOutboxJpaEntity entity) {
        if (entity == null) return null;
        return new StockOutboxMessage(
                entity.getId(),
                entity.getEventId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getTenantId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getAttempts() != null ? entity.getAttempts() : 0,
                entity.getNextAttemptAt(),
                entity.getLastError(),
                entity.getStatus() != null ? entity.getStatus() : StockOutboxStatus.PENDING,
                entity.getCreatedAt()
        );
    }

    public static StockOutboxJpaEntity toEntity(StockOutboxMessage domain) {
        if (domain == null) return null;
        return new StockOutboxJpaEntity(
                domain.getId(),
                domain.getAggregateType(),
                domain.getAggregateId(),
                domain.getTenantId(),
                domain.getEventId(),
                domain.getEventType(),
                domain.getPayload(),
                domain.getAttempts(),
                domain.getNextAttemptAt(),
                domain.getLastError(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
    }
}
