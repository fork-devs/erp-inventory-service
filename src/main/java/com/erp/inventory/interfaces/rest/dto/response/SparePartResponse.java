package com.erp.inventory.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SparePartResponse(
        UUID id,
        UUID tenantId,
        String sku,
        String description,
        int minStock,
        Instant createdAt
) {}
