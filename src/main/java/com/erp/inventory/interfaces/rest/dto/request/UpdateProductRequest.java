package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Category cannot be blank")
        @Size(max = 100, message = "Category cannot exceed 100 characters")
        String category,

        @NotBlank(message = "Name cannot be blank")
        @Size(max = 255, message = "Name cannot exceed 255 characters")
        String name,

        String description,

        @NotNull(message = "Sale price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Sale price cannot be negative")
        BigDecimal salePrice,

        @NotNull(message = "Minimum stock is required")
        @Min(value = 0, message = "Minimum stock cannot be negative")
        Integer minimumStock
) {}
