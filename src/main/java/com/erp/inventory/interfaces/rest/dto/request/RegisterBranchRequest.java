package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterBranchRequest(
        @NotBlank(message = "{validation.inventory.branch.name.not-blank}")
        String name,
        String address
) {}
