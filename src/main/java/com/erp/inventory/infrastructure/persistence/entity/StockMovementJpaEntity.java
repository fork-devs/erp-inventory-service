package com.erp.inventory.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
public class StockMovementJpaEntity {
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "spare_part_id", nullable = false)
    private UUID sparePartId;
    @Column(name = "branch_id", nullable = false)
    private UUID branchId;
    @Column(name = "movement_type", nullable = false, length = 20)
    private String movementType;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "reference_id")
    private UUID referenceId;
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;
    @Column(name = "created_at")
    private Instant createdAt;

    public StockMovementJpaEntity() {}

    public StockMovementJpaEntity(UUID id, UUID tenantId, UUID sparePartId, UUID branchId,
                                  String movementType, int quantity, UUID referenceId,
                                  String idempotencyKey, Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.sparePartId = sparePartId;
        this.branchId = branchId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
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
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public UUID getReferenceId() { return referenceId; }
    public void setReferenceId(UUID referenceId) { this.referenceId = referenceId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
