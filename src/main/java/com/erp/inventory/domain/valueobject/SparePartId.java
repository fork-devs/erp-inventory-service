package com.erp.inventory.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class SparePartId implements Serializable {
    private final UUID value;

    public SparePartId(UUID value) {
        if (value == null) throw new IllegalArgumentException("SparePartId no puede ser nulo");
        this.value = value;
    }

    public static SparePartId generate() { return new SparePartId(UUID.randomUUID()); }
    public static SparePartId fromString(String value) { return new SparePartId(UUID.fromString(value)); }

    public UUID getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SparePartId s)) return false;
        return Objects.equals(value, s.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value.toString(); }
}
