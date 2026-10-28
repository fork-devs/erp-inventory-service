package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RegisterSparePartRequest(
        @NotBlank(message = "{validation.inventory.spare-part.sku.not-blank}")
        String sku,
        @NotBlank(message = "{validation.inventory.spare-part.description.not-blank}")
        String description,
        @Min(value = 0, message = "{validation.inventory.spare-part.min-stock.min}")
        int minStock
) {}
