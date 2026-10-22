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
 * Lote de stock: las unidades que entraron juntas, con su coste real.
 *
 * <p>El saldo NO es la suma de los movimientos: eso dice cuántas unidades hay,
 * pero no cuánto valen. El coste de lo consumido se pierde en el momento en
 * que se consume, y el coste medio mezcla compras antiguas con recientes.
 * Cada entrada abre un lote, y las salidas y las reservas **consumen primero
 * el lote más antiguo** (FIFO).
 *
 * <p>La reserva se marca SOBRE el lote ({@code reservedQuantity}): así el
 * valor del stock reservado sale de la misma fila que el disponible, sin
 * tener que reconstruir el reparto por fechas.
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

    /** Unidades del lote que todavia no estan reservadas. */
    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    /**
     * Reserva unidades de este lote.
     *
     * @throws IllegalStateException si no quedan unidades libres.
     */
    public void reserve(int units) {
        if (units <= 0 || units > getAvailableQuantity()) {
            throw new IllegalStateException("El lote no tiene " + units + " unidades libres");
        }
        this.reservedQuantity = this.reservedQuantity + units;
    }

    /**
     * Devuelve unidades reservadas de este lote.
     */
    public void release(int units) {
        if (units <= 0 || units > reservedQuantity) {
            throw new IllegalStateException("El lote no tiene " + units + " unidades reservadas");
        }
        this.reservedQuantity = this.reservedQuantity - units;
    }

    /**
     * Consume unidades LIBRES del lote.
     */
    public void consume(int units) {
        if (units <= 0 || units > getAvailableQuantity()) {
            throw new IllegalStateException("El lote no tiene " + units + " unidades libres");
        }
        this.quantity = this.quantity - units;
    }

    /**
     * Consume unidades YA RESERVADAS del lote: la reserva se convierte en
     * consumo real.
     */
    public void consumeReserved(int units) {
        if (units <= 0 || units > reservedQuantity) {
            throw new IllegalStateException("El lote no tiene " + units + " unidades reservadas");
        }
        this.reservedQuantity = this.reservedQuantity - units;
        this.quantity = this.quantity - units;
    }

    /** Valor de las unidades todavia libres del lote. */
    public BigDecimal getAvailableValue() {
        return unitCost.multiply(BigDecimal.valueOf(getAvailableQuantity()));
    }

    /** Valor de las unidades reservadas del lote. */
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
