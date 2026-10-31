package com.erp.inventory.application.dto.command;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AddProductBatchCommand(
        UUID tenantId,
        UUID productId,
        String batchNumber,
        Integer quantity,
        BigDecimal purchasePrice,
        Instant expirationDate
) {}
