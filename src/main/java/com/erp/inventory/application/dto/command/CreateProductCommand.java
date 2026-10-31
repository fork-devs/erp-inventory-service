package com.erp.inventory.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductCommand(
        UUID tenantId,
        UUID branchId,
        String category,
        String name,
        String sku,
        String description,
        BigDecimal salePrice,
        Integer minimumStock
) {}
