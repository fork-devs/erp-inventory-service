package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.StockLot;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.util.List;

/**
 * Lotes de stock de un repuesto en una sucursal.
 *
 * <p>El consumo es FIFO: los lotes se toman del mas antiguo al mas reciente.
 * Para que dos reservas concurrentes no puedan sobre-reservar, los lotes se
 * **bloquean** antes de calcular el disponible: la segunda reserva espera, y
 * cuando obtiene el bloqueo ve el stock ya descontado.
 */
public interface StockLotRepositoryPort {

    /**
     * Lotes de un repuesto en una sucursal, **bloqueados** y en orden de
     * entrada.
     */
    List<StockLot> bloquearLotes(TenantId tenantId, SparePartId sparePartId, BranchId branchId);

    StockLot guardar(StockLot lot);

    List<StockLot> listar(TenantId tenantId, SparePartId sparePartId, BranchId branchId);
}
