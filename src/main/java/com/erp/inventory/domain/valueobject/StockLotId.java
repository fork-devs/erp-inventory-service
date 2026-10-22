package com.erp.inventory.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Identificador de un lote de stock.
 */
public final class StockLotId implements Serializable {

    private final UUID value;

    public StockLotId(UUID value) {
        if (value == null) throw new IllegalArgumentException("StockLotId no puede ser nulo");
        this.value = value;
    }

    public static StockLotId generate() { return new StockLotId(UUID.randomUUID()); }

    public UUID getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockLotId other)) return false;
        return Objects.equals(value, other.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value.toString(); }
}
