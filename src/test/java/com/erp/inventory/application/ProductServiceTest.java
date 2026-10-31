package com.erp.inventory.application;

import com.erp.inventory.application.dto.command.CreateProductCommand;
import com.erp.inventory.application.dto.command.UpdateProductCommand;
import com.erp.inventory.application.dto.result.ProductResult;
import com.erp.inventory.application.service.ProductService;
import com.erp.inventory.domain.exception.ActiveStockConflictException;
import com.erp.inventory.domain.exception.DuplicateProductSkuException;
import com.erp.inventory.domain.exception.ProductNotFoundException;
import com.erp.inventory.domain.model.Product;
import com.erp.inventory.domain.model.ProductBatch;
import com.erp.inventory.domain.port.ProductBatchRepositoryPort;
import com.erp.inventory.domain.port.ProductRepositoryPort;
import com.erp.inventory.domain.port.UnitOfWork;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.Money;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.TenantId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private ProductBatchRepositoryPort productBatchRepository;

    @Mock
    private UnitOfWork unitOfWork;

    private ProductService productService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(unitOfWork.callInTransaction(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(unitOfWork).runInTransaction(any());

        productService = new ProductService(productRepository, productBatchRepository, unitOfWork);
    }

    @Test
    @DisplayName("Should create product successfully when SKU is unique")
    void shouldCreateProductSuccessfully() {
        CreateProductCommand cmd = new CreateProductCommand(
                tenantId,
                branchId,
                "Filters",
                "Oil Filter",
                "FLT-001",
                "Description",
                BigDecimal.valueOf(45.0),
                5
        );

        when(productRepository.existsBySkuAndBranch(any(), any(), any())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResult result = productService.create(cmd);

        assertThat(result).isNotNull();
        assertThat(result.category()).isEqualTo("Filters");
        assertThat(result.name()).isEqualTo("Oil Filter");
        assertThat(result.sku()).isEqualTo("FLT-001");
        assertThat(result.currentStock()).isZero();
        assertThat(result.lowStockAlert()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw DuplicateProductSkuException when SKU already exists in branch")
    void shouldThrowWhenSkuExists() {
        CreateProductCommand cmd = new CreateProductCommand(
                tenantId,
                branchId,
                "Filters",
                "Oil Filter",
                "FLT-001",
                "Description",
                BigDecimal.valueOf(45.0),
                5
        );

        when(productRepository.existsBySkuAndBranch(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> productService.create(cmd))
                .isInstanceOf(DuplicateProductSkuException.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve product with populated batches and stock")
    void shouldGetProductWithBatches() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(
                new TenantId(tenantId),
                new BranchId(branchId),
                "Brakes",
                "Brake Pad",
                Sku.of("BRK-1"),
                "Desc",
                Money.of(100.0),
                4
        );

        when(productRepository.findById(eq(new ProductId(productId)), eq(new TenantId(tenantId))))
                .thenReturn(Optional.of(product));

        ProductBatch batch = ProductBatch.create(new TenantId(tenantId), product.getId(), "B1", 10, Money.of(60.0), null);
        when(productBatchRepository.findByProductId(eq(product.getId()), eq(new TenantId(tenantId))))
                .thenReturn(List.of(batch));

        ProductResult result = productService.getById(productId, tenantId);

        assertThat(result).isNotNull();
        assertThat(result.currentStock()).isEqualTo(10);
        assertThat(result.lowStockAlert()).isFalse(); // 10 > 4
        assertThat(result.batches()).hasSize(1);
    }

    @Test
    @DisplayName("Should update product details")
    void shouldUpdateProduct() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(
                new TenantId(tenantId),
                new BranchId(branchId),
                "Old",
                "OldName",
                Sku.of("SKU-1"),
                "OldDesc",
                Money.of(10.0),
                2
        );

        when(productRepository.findById(eq(new ProductId(productId)), eq(new TenantId(tenantId))))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductCommand cmd = new UpdateProductCommand(
                tenantId,
                productId,
                "NewCat",
                "NewName",
                "NewDesc",
                BigDecimal.valueOf(25.0),
                8
        );

        ProductResult updated = productService.update(cmd);

        assertThat(updated.category()).isEqualTo("NewCat");
        assertThat(updated.name()).isEqualTo("NewName");
        assertThat(updated.salePrice()).isEqualByComparingTo("25.0000");
        assertThat(updated.minimumStock()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should delete product when current stock is zero")
    void shouldDeleteProductWhenZeroStock() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(
                new TenantId(tenantId),
                new BranchId(branchId),
                "Cat",
                "Name",
                Sku.of("SKU-1"),
                "Desc",
                Money.of(10.0),
                0
        );

        when(productRepository.findById(eq(new ProductId(productId)), eq(new TenantId(tenantId))))
                .thenReturn(Optional.of(product));
        when(productBatchRepository.findByProductId(eq(product.getId()), eq(new TenantId(tenantId))))
                .thenReturn(List.of());

        productService.delete(productId, tenantId);

        assertThat(product.isActive()).isFalse();
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should reject product deletion when active stock remains")
    void shouldRejectDeletionWhenStockRemains() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(
                new TenantId(tenantId),
                new BranchId(branchId),
                "Cat",
                "Name",
                Sku.of("SKU-1"),
                "Desc",
                Money.of(10.0),
                0
        );

        ProductBatch batch = ProductBatch.create(new TenantId(tenantId), product.getId(), "B1", 5, Money.of(5.0), null);
        when(productRepository.findById(eq(new ProductId(productId)), eq(new TenantId(tenantId))))
                .thenReturn(Optional.of(product));
        when(productBatchRepository.findByProductId(eq(product.getId()), eq(new TenantId(tenantId))))
                .thenReturn(List.of(batch));

        assertThatThrownBy(() -> productService.delete(productId, tenantId))
                .isInstanceOf(ActiveStockConflictException.class);

        verify(productRepository, never()).save(any());
    }
}
