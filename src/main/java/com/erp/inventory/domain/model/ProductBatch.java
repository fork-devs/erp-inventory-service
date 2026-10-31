package com.erp.inventory.domain.model;

import com.erp.inventory.domain.exception.InvalidBatchException;
import com.erp.inventory.domain.valueobject.BatchId;
import com.erp.inventory.domain.valueobject.Money;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class ProductBatch implements Serializable {
    private final BatchId id;
    private final TenantId tenantId;
    private final ProductId productId;
    private final String batchNumber;
    private final int quantity;
    private int remainingQuantity;
    private final Money purchasePrice;
    private final Instant expirationDate;
    private final Instant createdAt;

    public ProductBatch(BatchId id,
                        TenantId tenantId,
                        ProductId productId,
                        String batchNumber,
                        int quantity,
                        int remainingQuantity,
                        Money purchasePrice,
                        Instant expirationDate,
                        Instant createdAt) {
        if (id == null || tenantId == null || productId == null || purchasePrice == null) {
            throw new IllegalArgumentException("Required fields cannot be null");
        }
        if (batchNumber == null || batchNumber.trim().isEmpty()) {
            throw new InvalidBatchException("Batch number cannot be empty");
        }
        if (quantity <= 0) {
            throw new InvalidBatchException("Batch quantity must be greater than zero");
        }
        if (remainingQuantity < 0 || remainingQuantity > quantity) {
            throw new InvalidBatchException("Remaining quantity must be between 0 and " + quantity);
        }
        this.id = id;
        this.tenantId = tenantId;
        this.productId = productId;
        this.batchNumber = batchNumber.trim();
        this.quantity = quantity;
        this.remainingQuantity = remainingQuantity;
        this.purchasePrice = purchasePrice;
        this.expirationDate = expirationDate;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static ProductBatch create(TenantId tenantId,
                                      ProductId productId,
                                      String batchNumber,
                                      int quantity,
                                      Money purchasePrice,
                                      Instant expirationDate) {
        return new ProductBatch(
                BatchId.generate(),
                tenantId,
                productId,
                batchNumber,
                quantity,
                quantity,
                purchasePrice,
                expirationDate,
                Instant.now()
        );
    }

    public void consume(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Consumed amount must be greater than zero");
        }
        if (amount > remainingQuantity) {
            throw new InvalidBatchException("Cannot consume " + amount + " units; only " + remainingQuantity + " available in batch");
        }
        this.remainingQuantity -= amount;
    }

    public boolean isAvailable() {
        return remainingQuantity > 0;
    }

    public BatchId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public ProductId getProductId() {
        return productId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getRemainingQuantity() {
        return remainingQuantity;
    }

    public Money getPurchasePrice() {
        return purchasePrice;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductBatch that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
