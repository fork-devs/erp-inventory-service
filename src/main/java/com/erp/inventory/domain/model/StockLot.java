package com.erp.inventory.domain.model;

import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.StockLotId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Stock lot: units received together, tracking their actual unit purchase cost.
 *
 * <p>Stock balance is NOT merely the sum of movements: summing indicates quantity,
 * but not valuation. Consumed unit costs are lost once consumed, and average costing
 * blends older and newer purchases. Each incoming movement opens a lot, and outgoing
 * movements and reservations **consume the oldest lot first** (FIFO).
 *
 * <p>Reservations are tracked directly ON the lot ({@code reservedQuantity}): this allows
 * reserved stock valuation to be derived from the same row as available stock without
 * reconstructing date-based allocations.
 */
public class StockLot implements Serializable {

    private final StockLotId id;
    private final TenantId tenantId;
    private final SparePartId sparePartId;
    private final BranchId branchId;
    private int quantity;
    private int reservedQuantity;
    private final BigDecimal unitCost;
    private final Instant entryDate;

    public StockLot(StockLotId id, TenantId tenantId, SparePartId sparePartId, BranchId branchId,
                     int quantity, int reservedQuantity, BigDecimal unitCost, Instant entryDate) {
        if (id == null || tenantId == null || sparePartId == null || branchId == null || unitCost == null) {
            throw new IllegalArgumentException("Campos obligatorios del lote no pueden ser nulos");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad del lote debe ser mayor a cero");
        }
        if (reservedQuantity < 0 || reservedQuantity > quantity) {
            throw new IllegalArgumentException("La cantidad reservada no puede ser negativa ni mayor que la del lote");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.sparePartId = sparePartId;
        this.branchId = branchId;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.unitCost = unitCost;
        this.entryDate = entryDate != null ? entryDate : Instant.now();
    }

    public static StockLot open(TenantId tenantId, SparePartId sparePartId, BranchId branchId,
                                 int quantity, BigDecimal unitCost, Instant entryDate) {
        return new StockLot(StockLotId.generate(), tenantId, sparePartId, branchId,
                quantity, 0, unitCost, entryDate);
    }

    /** Units in this lot that are not yet reserved. */
    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    /**
     * Reserves units from this lot.
     *
     * @param units quantity to reserve
     * @throws IllegalStateException if there are insufficient unreserved units.
     */
    public void reserve(int units) {
        if (units <= 0 || units > getAvailableQuantity()) {
            throw new IllegalStateException("El lote no tiene " + units + " unidades libres");
        }
        this.reservedQuantity = this.reservedQuantity + units;
    }

    /**
     * Releases reserved units back to this lot.
     *
     * @param units quantity to release
     */
    public void release(int units) {
        if (units <= 0 || units > reservedQuantity) {
            throw new IllegalStateException("El lote no tiene " + units + " unidades reservadas");
        }
        this.reservedQuantity = this.reservedQuantity - units;
    }

    /**
     * Consumes unreserved units directly from the lot.
     *
     * @param units quantity to consume
     */
    public void consume(int units) {
        if (units <= 0 || units > getAvailableQuantity()) {
            throw new IllegalStateException("El lote no tiene " + units + " unidades libres");
        }
        this.quantity = this.quantity - units;
    }

    /**
     * Consumes previously reserved units from the lot, converting the reservation
     * into actual consumption.
     *
     * @param units quantity to consume
     */
    public void consumeReserved(int units) {
        if (units <= 0 || units > reservedQuantity) {
            throw new IllegalStateException("El lote no tiene " + units + " unidades reservadas");
        }
        this.reservedQuantity = this.reservedQuantity - units;
        this.quantity = this.quantity - units;
    }

    /** Valuation of unreserved units remaining in this lot. */
    public BigDecimal getAvailableValue() {
        return unitCost.multiply(BigDecimal.valueOf(getAvailableQuantity()));
    }

    /** Valuation of reserved units in this lot. */
    public BigDecimal getReservedValue() {
        return unitCost.multiply(BigDecimal.valueOf(reservedQuantity));
    }

    public StockLotId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public SparePartId getSparePartId() { return sparePartId; }
    public BranchId getBranchId() { return branchId; }
    public int getQuantity() { return quantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public Instant getEntryDate() { return entryDate; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockLot l)) return false;
        return Objects.equals(id, l.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
