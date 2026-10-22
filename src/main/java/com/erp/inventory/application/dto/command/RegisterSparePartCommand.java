package com.erp.inventory.application.dto.command;

import java.util.UUID;

public record RegisterSparePartCommand(
        UUID tenantId,
        String sku,
        String description,
        int minStock
) {}
