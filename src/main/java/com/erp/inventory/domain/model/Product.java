package com.erp.inventory.domain.model;

import com.erp.inventory.domain.exception.ActiveStockConflictException;
import com.erp.inventory.domain.exception.InvalidBatchException;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.Money;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.TenantId;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Product implements Serializable {
    private final ProductId id;
    private final TenantId tenantId;
    private final BranchId branchId;
    private String category;
    private String name;
    private final Sku sku;
    private String description;
    private Money salePrice;
    private int minimumStock;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private final List<ProductBatch> batches;

    public Product(ProductId id,
                   TenantId tenantId,
                   BranchId branchId,
                   String category,
                   String name,
                   Sku sku,
                   String description,
                   Money salePrice,
                   int minimumStock,
                   boolean active,
                   Instant createdAt,
                   Instant updatedAt,
                   List<ProductBatch> batches) {
        if (id == null || tenantId == null || branchId == null || sku == null || salePrice == null) {
            throw new IllegalArgumentException("Required product fields cannot be null");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Product category cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (minimumStock < 0) {
            throw new IllegalArgumentException("Minimum stock cannot be negative");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.category = category.trim();
        this.name = name.trim();
        this.sku = sku;
        this.description = description != null ? description.trim() : "";
        this.salePrice = salePrice;
        this.minimumStock = minimumStock;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.batches = new ArrayList<>(batches != null ? batches : List.of());
    }

    public static Product create(TenantId tenantId,
                                 BranchId branchId,
                                 String category,
                                 String name,
                                 Sku sku,
                                 String description,
                                 Money salePrice,
                                 int minimumStock) {
        Instant now = Instant.now();
        return new Product(
                ProductId.generate(),
                tenantId,
                branchId,
                category,
                name,
                sku,
                description,
                salePrice,
                minimumStock,
                true,
                now,
                now,
                new ArrayList<>()
        );
    }

    public void update(String category, String name, String description, Money salePrice, int minimumStock) {
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Product category cannot be empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (salePrice == null) {
            throw new IllegalArgumentException("Sale price cannot be null");
        }
        if (minimumStock < 0) {
            throw new IllegalArgumentException("Minimum stock cannot be negative");
        }
        this.category = category.trim();
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";
        this.salePrice = salePrice;
        this.minimumStock = minimumStock;
        this.updatedAt = Instant.now();
    }

    public void addBatch(ProductBatch batch) {
        if (batch == null) {
            throw new IllegalArgumentException("Batch cannot be null");
        }
        if (!this.id.equals(batch.getProductId())) {
            throw new InvalidBatchException("Batch does not belong to this product");
        }
        this.batches.add(batch);
        this.updatedAt = Instant.now();
    }

    public int getCurrentStock() {
        return batches.stream()
                .mapToInt(ProductBatch::getRemainingQuantity)
                .sum();
    }

    public boolean isLowStockAlert() {
        return getCurrentStock() <= minimumStock;
    }

    public void deactivate() {
        int currentStock = getCurrentStock();
        if (currentStock > 0) {
            throw new ActiveStockConflictException(this.id.getValue(), currentStock);
        }
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public ProductId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public BranchId getBranchId() {
        return branchId;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public Sku getSku() {
        return sku;
    }

    public String getDescription() {
        return description;
    }

    public Money getSalePrice() {
        return salePrice;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ProductBatch> getBatches() {
        return Collections.unmodifiableList(batches);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
