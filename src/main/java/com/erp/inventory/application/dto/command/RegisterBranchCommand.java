package com.erp.inventory.application.dto.command;

import java.util.UUID;

public record RegisterBranchCommand(
        UUID tenantId,
        String name,
        String address
) {}
