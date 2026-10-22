package com.erp.inventory.interfaces.rest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BranchResponse(
        UUID id,
        UUID tenantId,
        String name,
        String address,
        Instant createdAt
) {}
