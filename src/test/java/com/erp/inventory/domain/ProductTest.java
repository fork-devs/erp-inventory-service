package com.erp.inventory.domain;

import com.erp.inventory.domain.exception.ActiveStockConflictException;
import com.erp.inventory.domain.exception.InvalidBatchException;
import com.erp.inventory.domain.model.Product;
import com.erp.inventory.domain.model.ProductBatch;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.Money;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    private final TenantId tenantId = TenantId.generate();
    private final BranchId branchId = BranchId.generate();

    @Test
    @DisplayName("Should create product with initial zero stock and low stock alert true")
    void shouldCreateProductWithInitialZeroStock() {
        Product product = Product.create(
                tenantId,
                branchId,
                "Filters",
                "Oil Filter Premium",
                Sku.of("FLT-001"),
                "High performance filter",
                Money.of(45.0),
                5
        );

        assertThat(product.getId()).isNotNull();
        assertThat(product.getTenantId()).isEqualTo(tenantId);
        assertThat(product.getBranchId()).isEqualTo(branchId);
        assertThat(product.getCategory()).isEqualTo("Filters");
        assertThat(product.getName()).isEqualTo("Oil Filter Premium");
        assertThat(product.getSku()).isEqualTo(Sku.of("FLT-001"));
        assertThat(product.getCurrentStock()).isZero();
        assertThat(product.isLowStockAlert()).isTrue();
        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should reject product with invalid parameters")
    void shouldRejectInvalidProductParameters() {
        assertThatThrownBy(() -> Product.create(tenantId, branchId, "", "Name", Sku.of("SKU"), "Desc", Money.of(10), 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Product.create(tenantId, branchId, "Cat", "", Sku.of("SKU"), "Desc", Money.of(10), 0))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Product.create(tenantId, branchId, "Cat", "Name", Sku.of("SKU"), "Desc", Money.of(10), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should dynamically update current stock and low stock alert when batches are added")
    void shouldUpdateStockWhenBatchesAdded() {
        Product product = Product.create(
                tenantId,
                branchId,
                "Brakes",
                "Ceramic Brake Pads",
                Sku.of("BRK-001"),
                "Front axle brake pads",
                Money.of(120.0),
                10
        );

        assertThat(product.isLowStockAlert()).isTrue();

        ProductBatch batch1 = ProductBatch.create(
                tenantId,
                product.getId(),
                "BATCH-01",
                6,
                Money.of(70.0),
                Instant.now().plus(365, ChronoUnit.DAYS)
        );
        product.addBatch(batch1);

        assertThat(product.getCurrentStock()).isEqualTo(6);
        assertThat(product.isLowStockAlert()).isTrue(); // 6 <= 10

        ProductBatch batch2 = ProductBatch.create(
                tenantId,
                product.getId(),
                "BATCH-02",
                10,
                Money.of(75.0),
                Instant.now().plus(365, ChronoUnit.DAYS)
        );
        product.addBatch(batch2);

        assertThat(product.getCurrentStock()).isEqualTo(16);
        assertThat(product.isLowStockAlert()).isFalse(); // 16 > 10
    }

    @Test
    @DisplayName("Should reject adding batch from a different product")
    void shouldRejectBatchFromDifferentProduct() {
        Product product = Product.create(
                tenantId,
                branchId,
                "Fluids",
                "Synthetic Oil",
                Sku.of("OIL-001"),
                "5W-30",
                Money.of(80.0),
                4
        );

        ProductBatch foreignBatch = ProductBatch.create(
                tenantId,
                ProductId.generate(),
                "BATCH-X",
                10,
                Money.of(50.0),
                null
        );

        assertThatThrownBy(() -> product.addBatch(foreignBatch))
                .isInstanceOf(InvalidBatchException.class);
    }

    @Test
    @DisplayName("Should deactivate product when stock is zero")
    void shouldDeactivateWhenZeroStock() {
        Product product = Product.create(
                tenantId,
                branchId,
                "Tools",
                "Wrench",
                Sku.of("TL-001"),
                "10mm wrench",
                Money.of(15.0),
                2
        );

        product.deactivate();
        assertThat(product.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw ActiveStockConflictException when deactivating product with remaining stock")
    void shouldRejectDeactivationWhenStockRemains() {
        Product product = Product.create(
                tenantId,
                branchId,
                "Belts",
                "Timing Belt",
                Sku.of("BLT-001"),
                "Rubber belt",
                Money.of(90.0),
                1
        );

        product.addBatch(ProductBatch.create(tenantId, product.getId(), "B-1", 5, Money.of(40.0), null));

        assertThatThrownBy(product::deactivate)
                .isInstanceOf(ActiveStockConflictException.class)
                .hasMessageContaining("Cannot delete product");
    }

    @Test
    @DisplayName("Should update product fields correctly")
    void shouldUpdateProductFields() {
        Product product = Product.create(
                tenantId,
                branchId,
                "OldCat",
                "OldName",
                Sku.of("SKU-OLD"),
                "OldDesc",
                Money.of(50.0),
                5
        );

        product.update("NewCat", "NewName", "NewDesc", Money.of(60.0), 10);

        assertThat(product.getCategory()).isEqualTo("NewCat");
        assertThat(product.getName()).isEqualTo("NewName");
        assertThat(product.getDescription()).isEqualTo("NewDesc");
        assertThat(product.getSalePrice()).isEqualTo(Money.of(60.0));
        assertThat(product.getMinimumStock()).isEqualTo(10);
    }
}
