package com.erp.inventory.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record SparePartResult(
        UUID id,
        UUID tenantId,
        String sku,
        String description,
        int minStock,
        Instant createdAt
) {}
