package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.StockMovement;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface StockMovementRepositoryPort {

    StockMovement save(StockMovement movement);

    /**
     * Reclama la clave de idempotencia registrando el movimiento.
     *
     * <p>La reclamacion es la propia insercion: la unicidad de
     * {@code (tenant_id, idempotency_key)} arbitra entre las peticiones
     * concurrentes con la misma clave. Entre consultar la clave y registrar el
     * movimiento hay una ventana en la que otra peticion con la misma clave
     * pasaria la comprobacion, y las dos registrarian su movimiento.
     *
     * @return vacio si la reclamacion es nueva; el movimiento ya registrado si
     *         la clave estaba reclamada.
     */
    Optional<StockMovement> claim(StockMovement movement);

    List<StockMovement> findBySparePartAndBranch(SparePartId sparePartId, BranchId branchId, TenantId tenantId);

    List<StockMovement> findBySparePart(SparePartId sparePartId, TenantId tenantId);

    Optional<StockMovement> findByIdempotencyKey(String idempotencyKey, TenantId tenantId);
}
