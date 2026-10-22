package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RegisterSparePartRequest(
        @NotBlank String sku,
        @NotBlank String description,
        @Min(0) int minStock
) {}
