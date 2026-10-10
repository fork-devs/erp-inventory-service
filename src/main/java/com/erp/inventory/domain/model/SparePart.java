package com.erp.inventory.domain.model;

import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class SparePart implements Serializable {
    private final SparePartId id;
    private final TenantId tenantId;
    private final Sku sku;
    private String description;
    private int minStock;
    private final Instant createdAt;

    public SparePart(SparePartId id, TenantId tenantId, Sku sku, String description, int minStock, Instant createdAt) {
        if (id == null || tenantId == null || sku == null || description == null) {
            throw new IllegalArgumentException("Campos obligatorios del repuesto no pueden ser nulos");
        }
        if (description.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del repuesto no puede estar vacía");
        }
        if (minStock < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.sku = sku;
        this.description = description.trim();
        this.minStock = minStock;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static SparePart create(TenantId tenantId, Sku sku, String description, int minStock) {
        return new SparePart(SparePartId.generate(), tenantId, sku, description, minStock, Instant.now());
    }

    public void update(String description, int minStock) {
        if (description != null && !description.trim().isEmpty()) this.description = description.trim();
        if (minStock >= 0) this.minStock = minStock;
    }

    public SparePartId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public Sku getSku() { return sku; }
    public String getDescription() { return description; }
    public int getMinStock() { return minStock; }
    public Instant getCreatedAt() { return createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SparePart s)) return false;
        return Objects.equals(id, s.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
