package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.Product;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product product);

    Optional<Product> findById(ProductId id, TenantId tenantId);

    Optional<Product> findBySkuAndBranch(Sku sku, BranchId branchId, TenantId tenantId);

    List<Product> findByBranch(BranchId branchId, TenantId tenantId);

    List<Product> findByBranchAndCategory(BranchId branchId, String category, TenantId tenantId);

    boolean existsBySkuAndBranch(Sku sku, BranchId branchId, TenantId tenantId);
}
