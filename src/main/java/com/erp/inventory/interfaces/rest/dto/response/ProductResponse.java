package com.erp.inventory.interfaces.rest.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
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
        List<ProductBatchResponse> batches,
        String imageUrl
) {
    public ProductResponse(
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
            List<ProductBatchResponse> batches
    ) {
        this(id, branchId, category, name, sku, description, salePrice, minimumStock, currentStock, lowStockAlert, batches, null);
    }
}
