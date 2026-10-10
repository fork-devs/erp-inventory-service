package com.erp.inventory.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;

public final class Sku implements Serializable {
    private final String value;

    public Sku(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El SKU no puede estar vacío");
        }
        this.value = value.trim().toUpperCase();
    }

    public static Sku of(String value) { return new Sku(value); }

    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sku s)) return false;
        return Objects.equals(value, s.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
