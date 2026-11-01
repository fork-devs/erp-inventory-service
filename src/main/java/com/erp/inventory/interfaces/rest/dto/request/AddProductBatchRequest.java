package com.erp.inventory.interfaces.rest.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record AddProductBatchRequest(
        @NotBlank(message = "Batch number cannot be blank")
        @Size(max = 100, message = "Batch number cannot exceed 100 characters")
        String batchNumber,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Purchase price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Purchase price cannot be negative")
        BigDecimal purchasePrice,

        Instant expirationDate
) {}
