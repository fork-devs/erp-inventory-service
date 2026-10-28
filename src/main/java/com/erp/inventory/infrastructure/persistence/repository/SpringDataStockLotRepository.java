package com.erp.inventory.infrastructure.persistence.repository;

import com.erp.inventory.infrastructure.persistence.entity.StockLotJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for stock lots.
 *
 * <p>Pessimistic locking guarantees that concurrent reservations for the same spare part
 * serialize, ensuring the subsequent transaction reads updated stock levels upon acquiring the lock.
 */
public interface SpringDataStockLotRepository extends JpaRepository<StockLotJpaEntity, UUID> {

    /**
     * Retrieves stock lots for a spare part at a branch, pessimistically locked in FIFO order.
     *
     * <p>Uses row-level locking ({@code FOR UPDATE}) rather than table locking, preventing
     * contention between distinct spare parts.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM StockLotJpaEntity l "
            + "WHERE l.tenantId = :tenantId AND l.sparePartId = :sparePartId AND l.branchId = :branchId "
            + "ORDER BY l.entryDate ASC")
    List<StockLotJpaEntity> bloquearLotes(@Param("tenantId") UUID tenantId,
                                          @Param("sparePartId") UUID sparePartId,
                                          @Param("branchId") UUID branchId);

    List<StockLotJpaEntity> findByTenantIdAndSparePartIdAndBranchIdOrderByEntryDateAsc(
            UUID tenantId, UUID sparePartId, UUID branchId);
}
