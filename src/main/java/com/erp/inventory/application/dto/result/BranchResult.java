package com.erp.inventory.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record BranchResult(
        UUID id,
        UUID tenantId,
        String name,
        String address,
        Instant createdAt
) {}
