package com.erp.inventory.domain.model;

import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class Branch implements Serializable {
    private final BranchId id;
    private final TenantId tenantId;
    private String name;
    private String address;
    private final Instant createdAt;

    public Branch(BranchId id, TenantId tenantId, String name, String address, Instant createdAt) {
        if (id == null || tenantId == null || name == null) {
            throw new IllegalArgumentException("Campos obligatorios de la sucursal no pueden ser nulos");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la sucursal no puede estar vacío");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.name = name.trim();
        this.address = address;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Branch create(TenantId tenantId, String name, String address) {
        return new Branch(BranchId.generate(), tenantId, name, address, Instant.now());
    }

    public BranchId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Instant getCreatedAt() { return createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Branch b)) return false;
        return Objects.equals(id, b.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
