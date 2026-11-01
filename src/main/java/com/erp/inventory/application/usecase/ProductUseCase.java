package com.erp.inventory.application.usecase;

import com.erp.inventory.application.dto.command.CreateProductCommand;
import com.erp.inventory.application.dto.command.UpdateProductCommand;
import com.erp.inventory.application.dto.result.ProductResult;

import java.util.List;
import java.util.UUID;

public interface ProductUseCase {
    ProductResult create(CreateProductCommand command);

    ProductResult getById(UUID productId, UUID tenantId);

    List<ProductResult> listByBranch(UUID branchId, UUID tenantId, String category, Boolean lowStockOnly);

    ProductResult update(UpdateProductCommand command);

    void delete(UUID productId, UUID tenantId);
}
