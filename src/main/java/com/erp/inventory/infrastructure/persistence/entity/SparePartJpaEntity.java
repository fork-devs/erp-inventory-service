package com.erp.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spare_parts")
public class SparePartJpaEntity {
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(nullable = false, length = 100)
    private String sku;
    @Column(nullable = false)
    private String description;
    @Column(name = "min_stock", nullable = false)
    private int minStock;
    @Column(name = "created_at")
    private Instant createdAt;

    public SparePartJpaEntity() {}

    public SparePartJpaEntity(UUID id, UUID tenantId, String sku, String description, int minStock, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.sku = sku;
        this.description = description;
        this.minStock = minStock;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMinStock() { return minStock; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
