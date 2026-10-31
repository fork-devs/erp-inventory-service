package com.erp.inventory.domain;

import com.erp.inventory.domain.exception.InvalidBatchException;
import com.erp.inventory.domain.model.ProductBatch;
import com.erp.inventory.domain.valueobject.BatchId;
import com.erp.inventory.domain.valueobject.Money;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductBatchTest {

    private final TenantId tenantId = TenantId.generate();
    private final ProductId productId = ProductId.generate();

    @Test
    @DisplayName("Should create batch and consume quantity properly")
    void shouldCreateAndConsumeBatch() {
        Instant exp = Instant.now().plus(180, ChronoUnit.DAYS);
        ProductBatch batch = ProductBatch.create(tenantId, productId, "LOT-99", 20, Money.of(30.0), exp);

        assertThat(batch.getId()).isNotNull();
        assertThat(batch.getTenantId()).isEqualTo(tenantId);
        assertThat(batch.getProductId()).isEqualTo(productId);
        assertThat(batch.getBatchNumber()).isEqualTo("LOT-99");
        assertThat(batch.getQuantity()).isEqualTo(20);
        assertThat(batch.getRemainingQuantity()).isEqualTo(20);
        assertThat(batch.isAvailable()).isTrue();

        batch.consume(8);
        assertThat(batch.getRemainingQuantity()).isEqualTo(12);

        batch.consume(12);
        assertThat(batch.getRemainingQuantity()).isZero();
        assertThat(batch.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should reject invalid batch quantities or overconsumption")
    void shouldRejectInvalidQuantities() {
        assertThatThrownBy(() -> ProductBatch.create(tenantId, productId, "LOT-1", 0, Money.of(10), null))
                .isInstanceOf(InvalidBatchException.class);

        assertThatThrownBy(() -> ProductBatch.create(tenantId, productId, "", 10, Money.of(10), null))
                .isInstanceOf(InvalidBatchException.class);

        ProductBatch batch = ProductBatch.create(tenantId, productId, "LOT-1", 5, Money.of(10), null);
        assertThatThrownBy(() -> batch.consume(6))
                .isInstanceOf(InvalidBatchException.class);
    }
}
