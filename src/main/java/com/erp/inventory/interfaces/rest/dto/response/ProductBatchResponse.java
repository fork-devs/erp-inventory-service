package com.erp.inventory.interfaces.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductBatchResponse(
        UUID id,
        String batchNumber,
        Integer quantity,
        Integer remainingQuantity,
        BigDecimal purchasePrice,
        Instant expirationDate,
        Instant createdAt
) {}
