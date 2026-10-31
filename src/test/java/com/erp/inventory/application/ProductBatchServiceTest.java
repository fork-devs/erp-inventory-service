package com.erp.inventory.application;

import com.erp.inventory.application.dto.command.AddProductBatchCommand;
import com.erp.inventory.application.dto.result.ProductBatchResult;
import com.erp.inventory.application.service.ProductBatchService;
import com.erp.inventory.domain.exception.InvalidBatchException;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductBatchServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;

    @Mock
    private ProductBatchRepositoryPort productBatchRepository;

    @Mock
    private UnitOfWork unitOfWork;

    private ProductBatchService batchService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(unitOfWork.callInTransaction(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        batchService = new ProductBatchService(productRepository, productBatchRepository, unitOfWork);
    }

    @Test
    @DisplayName("Should add batch to product successfully")
    void shouldAddBatchSuccessfully() {
        Product product = new Product(
                new ProductId(productId),
                new TenantId(tenantId),
                BranchId.generate(),
                "Oil",
                "Motor Oil",
                Sku.of("OIL-1"),
                "Desc",
                Money.of(50.0),
                5,
                true,
                Instant.now(),
                Instant.now(),
                new java.util.ArrayList<>()
        );

        when(productRepository.findById(eq(new ProductId(productId)), eq(new TenantId(tenantId))))
                .thenReturn(Optional.of(product));
        when(productBatchRepository.findByProductIdAndBatchNumber(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(productBatchRepository.save(any(ProductBatch.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AddProductBatchCommand cmd = new AddProductBatchCommand(
                tenantId,
                productId,
                "LOT-001",
                25,
                BigDecimal.valueOf(32.5),
                Instant.now().plusSeconds(86400)
        );

        ProductBatchResult result = batchService.addBatch(cmd);

        assertThat(result).isNotNull();
        assertThat(result.batchNumber()).isEqualTo("LOT-001");
        assertThat(result.quantity()).isEqualTo(25);
        assertThat(result.remainingQuantity()).isEqualTo(25);
        verify(productBatchRepository).save(any(ProductBatch.class));
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should reject batch with duplicate batch number for same product")
    void shouldRejectDuplicateBatchNumber() {
        Product product = new Product(
                new ProductId(productId),
                new TenantId(tenantId),
                BranchId.generate(),
                "Oil",
                "Motor Oil",
                Sku.of("OIL-1"),
                "Desc",
                Money.of(50.0),
                5,
                true,
                Instant.now(),
                Instant.now(),
                new java.util.ArrayList<>()
        );

        when(productRepository.findById(eq(new ProductId(productId)), eq(new TenantId(tenantId))))
                .thenReturn(Optional.of(product));

        ProductBatch existing = ProductBatch.create(new TenantId(tenantId), product.getId(), "LOT-001", 10, Money.of(10), null);
        when(productBatchRepository.findByProductIdAndBatchNumber(any(), eq("LOT-001"), any()))
                .thenReturn(Optional.of(existing));

        AddProductBatchCommand cmd = new AddProductBatchCommand(
                tenantId,
                productId,
                "LOT-001",
                25,
                BigDecimal.valueOf(32.5),
                null
        );

        assertThatThrownBy(() -> batchService.addBatch(cmd))
                .isInstanceOf(InvalidBatchException.class)
                .hasMessageContaining("Batch number already exists");

        verify(productBatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should list batches for existing product")
    void shouldListBatchesForProduct() {
        Product product = new Product(
                new ProductId(productId),
                new TenantId(tenantId),
                BranchId.generate(),
                "Oil",
                "Motor Oil",
                Sku.of("OIL-1"),
                "Desc",
                Money.of(50.0),
                5,
                true,
                Instant.now(),
                Instant.now(),
                new java.util.ArrayList<>()
        );

        when(productRepository.findById(eq(new ProductId(productId)), eq(new TenantId(tenantId))))
                .thenReturn(Optional.of(product));

        ProductBatch b1 = ProductBatch.create(new TenantId(tenantId), product.getId(), "B1", 10, Money.of(10), null);
        ProductBatch b2 = ProductBatch.create(new TenantId(tenantId), product.getId(), "B2", 15, Money.of(12), null);

        when(productBatchRepository.findByProductId(eq(new ProductId(productId)), eq(new TenantId(tenantId))))
                .thenReturn(List.of(b1, b2));

        List<ProductBatchResult> batches = batchService.listBatches(productId, tenantId);

        assertThat(batches).hasSize(2);
    }
}
