package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterBranchRequest(
        @NotBlank String name,
        String address
) {}
