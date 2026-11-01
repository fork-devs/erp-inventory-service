package com.erp.inventory.application.usecase;

import com.erp.inventory.application.dto.command.AddProductBatchCommand;
import com.erp.inventory.application.dto.result.ProductBatchResult;

import java.util.List;
import java.util.UUID;

public interface ProductBatchUseCase {
    ProductBatchResult addBatch(AddProductBatchCommand command);

    List<ProductBatchResult> listBatches(UUID productId, UUID tenantId);
}
