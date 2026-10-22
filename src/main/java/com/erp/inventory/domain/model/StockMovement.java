package com.erp.inventory.domain.model;

import com.erp.inventory.domain.valueobject.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class StockMovement implements Serializable {
    private final StockMovementId id;
    private final TenantId tenantId;
    private final SparePartId sparePartId;
    private final BranchId branchId;
    private final MovementType movementType;
    private final Quantity quantity;
    private final UUID referenceId;
    private final IdempotencyKey idempotencyKey;
    /** Coste real de la operacion. Es informativo: la valoracion sale de los lotes. */
    private final BigDecimal unitCost;
    private final Instant createdAt;

    public StockMovement(StockMovementId id, TenantId tenantId, SparePartId sparePartId,
                         BranchId branchId, MovementType movementType, Quantity quantity,
                         UUID referenceId, IdempotencyKey idempotencyKey, BigDecimal unitCost,
                         Instant createdAt) {
        if (id == null || tenantId == null || sparePartId == null || branchId == null || movementType == null || quantity == null) {
            throw new IllegalArgumentException("Campos obligatorios del movimiento de stock no pueden ser nulos");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.sparePartId = sparePartId;
        this.branchId = branchId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.referenceId = referenceId;
        this.idempotencyKey = idempotencyKey;
        this.unitCost = unitCost;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StockMovement create(TenantId tenantId, SparePartId sparePartId, BranchId branchId,
                                      MovementType movementType, Quantity quantity, UUID referenceId,
                                      IdempotencyKey idempotencyKey) {
        return create(tenantId, sparePartId, branchId, movementType, quantity, referenceId,
                idempotencyKey, null);
    }

    public static StockMovement create(TenantId tenantId, SparePartId sparePartId, BranchId branchId,
                                      MovementType movementType, Quantity quantity, UUID referenceId,
                                      IdempotencyKey idempotencyKey, BigDecimal unitCost) {
        return new StockMovement(StockMovementId.generate(), tenantId, sparePartId, branchId,
                movementType, quantity, referenceId, idempotencyKey, unitCost, Instant.now());
    }

    public StockMovementId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public SparePartId getSparePartId() { return sparePartId; }
    public BranchId getBranchId() { return branchId; }
    public MovementType getMovementType() { return movementType; }
    public Quantity getQuantity() { return quantity; }
    public UUID getReferenceId() { return referenceId; }
    public IdempotencyKey getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getUnitCost() { return unitCost; }
    public Instant getCreatedAt() { return createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockMovement m)) return false;
        return Objects.equals(id, m.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
