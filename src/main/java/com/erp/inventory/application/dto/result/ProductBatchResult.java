package com.erp.inventory.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductBatchResult(
        UUID id,
        String batchNumber,
        Integer quantity,
        Integer remainingQuantity,
        BigDecimal purchasePrice,
        Instant expirationDate,
        Instant createdAt
) {}
