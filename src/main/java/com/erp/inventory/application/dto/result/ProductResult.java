package com.erp.inventory.application.dto.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResult(
        UUID id,
        UUID branchId,
        String category,
        String name,
        String sku,
        String description,
        BigDecimal salePrice,
        Integer minimumStock,
        Integer currentStock,
        Boolean lowStockAlert,
        Boolean active,
        List<ProductBatchResult> batches
) {}
