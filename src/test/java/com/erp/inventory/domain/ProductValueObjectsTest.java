package com.erp.inventory.domain;

import com.erp.inventory.domain.valueobject.BatchId;
import com.erp.inventory.domain.valueobject.Money;
import com.erp.inventory.domain.valueobject.ProductId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductValueObjectsTest {

    @Test
    @DisplayName("Should validate ProductId methods and invariants")
    void shouldValidateProductId() {
        UUID uuid = UUID.randomUUID();
        ProductId id1 = ProductId.of(uuid);
        ProductId id2 = ProductId.fromString(uuid.toString());
        ProductId id3 = ProductId.generate();

        assertThat(id1.getValue()).isEqualTo(uuid);
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1.toString()).isEqualTo(uuid.toString());

        assertThatThrownBy(() -> new ProductId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate BatchId methods and invariants")
    void shouldValidateBatchId() {
        UUID uuid = UUID.randomUUID();
        BatchId id1 = BatchId.of(uuid);
        BatchId id2 = BatchId.fromString(uuid.toString());
        BatchId id3 = BatchId.generate();

        assertThat(id1.getValue()).isEqualTo(uuid);
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isNotEqualTo(id3);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        assertThat(id1.toString()).isEqualTo(uuid.toString());

        assertThatThrownBy(() -> new BatchId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate Money arithmetic, equality, and formatting")
    void shouldValidateMoney() {
        Money m1 = Money.of(BigDecimal.valueOf(25.5));
        Money m2 = Money.of(25.5);
        Money m3 = Money.of(10L);
        Money zero = Money.zero();

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        assertThat(m1).isNotEqualTo(m3);
        assertThat(zero.getAmount()).isEqualByComparingTo("0.0000");

        Money sum = m1.add(m3);
        assertThat(sum.getAmount()).isEqualByComparingTo("35.5000");

        Money multiplied = m3.multiply(3);
        assertThat(multiplied.getAmount()).isEqualByComparingTo("30.0000");

        assertThat(m1.toString()).isEqualTo("25.5000");

        assertThatThrownBy(() -> new Money(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Money.of(-5.0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> m1.add(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
