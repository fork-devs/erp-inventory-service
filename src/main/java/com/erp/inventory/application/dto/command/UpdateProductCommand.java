package com.erp.inventory.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductCommand(
        UUID tenantId,
        UUID productId,
        String category,
        String name,
        String description,
        BigDecimal salePrice,
        Integer minimumStock
) {}
