package com.erp.inventory.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class BatchId implements Serializable {
    private final UUID value;

    public BatchId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("BatchId cannot be null");
        }
        this.value = value;
    }

    public static BatchId generate() {
        return new BatchId(UUID.randomUUID());
    }

    public static BatchId of(UUID value) {
        return new BatchId(value);
    }

    public static BatchId fromString(String value) {
        return new BatchId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BatchId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
