package com.erp.inventory.infrastructure.persistence.repository;

import com.erp.inventory.domain.model.StockOutboxStatus;
import com.erp.inventory.infrastructure.persistence.entity.StockOutboxJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataStockOutboxRepository extends JpaRepository<StockOutboxJpaEntity, UUID> {

    /**
     * Retrieves pending messages whose scheduled retry timestamp has arrived.
     *
     * <p>Uses {@code FOR UPDATE SKIP LOCKED} to ensure exclusive claiming across concurrent
     * relay instances, preventing duplicate processing of the same message.
     */
    @Query(value = "SELECT * FROM stock_outbox "
            + "WHERE status = 'PENDING' AND next_attempt_at <= :now "
            + "ORDER BY next_attempt_at ASC "
            + "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<StockOutboxJpaEntity> findPendingForUpdate(@Param("now") Instant now, Pageable pageable);

    @Query(value = "SELECT * FROM stock_outbox "
            + "WHERE status = 'DEAD_LETTER' AND (:tenantId IS NULL OR tenant_id = :tenantId) "
            + "ORDER BY created_at DESC",
            nativeQuery = true)
    List<StockOutboxJpaEntity> findDeadLettered(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT o FROM StockOutboxJpaEntity o WHERE o.id = :id")
    StockOutboxJpaEntity findByIdForUpdate(@Param("id") UUID id);
}
