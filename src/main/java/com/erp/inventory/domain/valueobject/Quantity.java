package com.erp.inventory.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;

public final class Quantity implements Serializable {
    private final int value;

    public Quantity(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }
        this.value = value;
    }

    public static Quantity of(int value) { return new Quantity(value); }

    public int getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantity q)) return false;
        return value == q.value;
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return String.valueOf(value); }
}
