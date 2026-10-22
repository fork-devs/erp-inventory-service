package com.erp.inventory.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class StockMovementId implements Serializable {
    private final UUID value;

    public StockMovementId(UUID value) {
        if (value == null) throw new IllegalArgumentException("StockMovementId no puede ser nulo");
        this.value = value;
    }

    public static StockMovementId generate() { return new StockMovementId(UUID.randomUUID()); }
    public static StockMovementId fromString(String value) { return new StockMovementId(UUID.fromString(value)); }

    public UUID getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockMovementId s)) return false;
        return Objects.equals(value, s.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value.toString(); }
}
