package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.SparePart;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface SparePartRepositoryPort {
    SparePart save(SparePart sparePart);
    Optional<SparePart> findById(SparePartId id, TenantId tenantId);
    List<SparePart> findAllByTenantId(TenantId tenantId);
    boolean existsBySku(String sku, TenantId tenantId);
    boolean existsById(SparePartId id, TenantId tenantId);
}
