package com.erp.inventory.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;

public final class IdempotencyKey implements Serializable {
    private final String value;

    public IdempotencyKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("La clave de idempotencia no puede estar vacía");
        }
        this.value = value.trim();
    }

    public static IdempotencyKey of(String value) { return new IdempotencyKey(value); }

    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdempotencyKey k)) return false;
        return Objects.equals(value, k.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
