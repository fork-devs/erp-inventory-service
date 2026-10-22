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
 * Acceso a los lotes de stock.
 *
 * <p>El bloqueo de los lotes es lo que convierte la comprobacion del
 * disponible en una GARANTIA: dos reservas concurrentes del mismo repuesto se
 * serializan, y la segunda ve el stock ya descontado cuando obtiene el
 * bloqueo.
 */
public interface SpringDataStockLotRepository extends JpaRepository<StockLotJpaEntity, UUID> {

    /**
     * Lotes de un repuesto en una sucursal, bloqueados y en orden de entrada.
     *
     * <p>El bloqueo es por fila ({@code FOR UPDATE}) y no de tabla: las
     * reservas de otros repuestos no se bloquean entre si.
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
