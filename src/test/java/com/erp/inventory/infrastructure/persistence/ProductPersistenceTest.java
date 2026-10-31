package com.erp.inventory.infrastructure.persistence;

import com.erp.inventory.domain.model.Branch;
import com.erp.inventory.domain.model.Product;
import com.erp.inventory.domain.model.ProductBatch;
import com.erp.inventory.domain.port.BranchRepositoryPort;
import com.erp.inventory.domain.port.ProductBatchRepositoryPort;
import com.erp.inventory.domain.port.ProductRepositoryPort;
import com.erp.inventory.domain.port.UnitOfWork;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.Money;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.rls.TenantContextHolder;
import com.erp.inventory.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPersistenceTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProductRepositoryPort productRepository;

    @Autowired
    private ProductBatchRepositoryPort productBatchRepository;

    @Autowired
    private BranchRepositoryPort branchRepository;

    @Autowired
    private UnitOfWork unitOfWork;

    private UUID tallerA;
    private UUID tallerB;
    private BranchId branchA;
    private BranchId branchB;

    @BeforeEach
    void setUp() {
        tallerA = UUID.randomUUID();
        tallerB = UUID.randomUUID();

        TenantContextHolder.setTenantId(tallerA);
        unitOfWork.runInTransaction(() -> {
            Branch b = Branch.create(new TenantId(tallerA), "Branch Lima", "Av. Principal 123");
            branchRepository.save(b);
            branchA = b.getId();
        });
        TenantContextHolder.clear();

        TenantContextHolder.setTenantId(tallerB);
        unitOfWork.runInTransaction(() -> {
            Branch b = Branch.create(new TenantId(tallerB), "Branch Arequipa", "Av. Grau 456");
            branchRepository.save(b);
            branchB = b.getId();
        });
        TenantContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Should persist product and batch for taller A and enforce RLS against taller B")
    void shouldPersistAndEnforceRls() {
        TenantContextHolder.setTenantId(tallerA);
        ProductId prodId = unitOfWork.callInTransaction(() -> {
            Product p = Product.create(
                    new TenantId(tallerA),
                    branchA,
                    "Filters",
                    "Air Filter",
                    Sku.of("AF-001"),
                    "Engine air filter",
                    Money.of(55.0),
                    3
            );
            Product saved = productRepository.save(p);

            ProductBatch batch = ProductBatch.create(
                    new TenantId(tallerA),
                    saved.getId(),
                    "BATCH-1",
                    10,
                    Money.of(30.0),
                    Instant.now().plusSeconds(86400)
            );
            productBatchRepository.save(batch);
            return saved.getId();
        });

        // Verify taller A sees the product and batch
        unitOfWork.runInTransaction(() -> {
            Optional<Product> found = productRepository.findById(prodId, new TenantId(tallerA));
            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("Air Filter");

            List<ProductBatch> batches = productBatchRepository.findByProductId(prodId, new TenantId(tallerA));
            assertThat(batches).hasSize(1);
            assertThat(batches.get(0).getBatchNumber()).isEqualTo("BATCH-1");
        });
        TenantContextHolder.clear();

        // Switch to taller B -> Should NOT see taller A's product or batches
        TenantContextHolder.setTenantId(tallerB);
        unitOfWork.runInTransaction(() -> {
            Optional<Product> foreign = productRepository.findById(prodId, new TenantId(tallerB));
            assertThat(foreign).isEmpty();

            List<ProductBatch> foreignBatches = productBatchRepository.findByProductId(prodId, new TenantId(tallerB));
            assertThat(foreignBatches).isEmpty();

            // Taller B can use the same SKU in its branch without conflict
            Product bProduct = Product.create(
                    new TenantId(tallerB),
                    branchB,
                    "Filters",
                    "Air Filter B",
                    Sku.of("AF-001"),
                    "Desc",
                    Money.of(60.0),
                    2
            );
            Product savedB = productRepository.save(bProduct);
            assertThat(savedB.getId()).isNotNull();
        });
    }
}
