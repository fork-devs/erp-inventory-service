package com.erp.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a stock lot with its units and unit cost.
 */
@Entity
@Table(name = "stock_lots")
public class StockLotJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "spare_part_id", nullable = false)
    private UUID sparePartId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "unit_cost", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "entry_date", nullable = false)
    private Instant entryDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public StockLotJpaEntity() {
    }

    public StockLotJpaEntity(UUID id, UUID tenantId, UUID sparePartId, UUID branchId,
                             int quantity, int reservedQuantity, BigDecimal unitCost,
                             Instant entryDate, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.sparePartId = sparePartId;
        this.branchId = branchId;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.unitCost = unitCost;
        this.entryDate = entryDate;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getSparePartId() { return sparePartId; }
    public void setSparePartId(UUID sparePartId) { this.sparePartId = sparePartId; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public Instant getEntryDate() { return entryDate; }
    public void setEntryDate(Instant entryDate) { this.entryDate = entryDate; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
