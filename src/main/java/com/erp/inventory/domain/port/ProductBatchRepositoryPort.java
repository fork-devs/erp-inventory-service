package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.ProductBatch;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface ProductBatchRepositoryPort {
    ProductBatch save(ProductBatch batch);

    List<ProductBatch> findByProductId(ProductId productId, TenantId tenantId);

    Optional<ProductBatch> findByProductIdAndBatchNumber(ProductId productId, String batchNumber, TenantId tenantId);
}
