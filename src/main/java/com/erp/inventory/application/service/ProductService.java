package com.erp.inventory.application.service;

import com.erp.inventory.application.dto.command.CreateProductCommand;
import com.erp.inventory.application.dto.command.UpdateProductCommand;
import com.erp.inventory.application.dto.result.ProductBatchResult;
import com.erp.inventory.application.dto.result.ProductResult;
import com.erp.inventory.application.usecase.ProductUseCase;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService implements ProductUseCase {

    private final ProductRepositoryPort productRepository;
    private final ProductBatchRepositoryPort productBatchRepository;
    private final UnitOfWork unitOfWork;

    public ProductService(ProductRepositoryPort productRepository,
                          ProductBatchRepositoryPort productBatchRepository,
                          UnitOfWork unitOfWork) {
        this.productRepository = productRepository;
        this.productBatchRepository = productBatchRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public ProductResult create(CreateProductCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            BranchId branchId = new BranchId(cmd.branchId());
            Sku sku = Sku.of(cmd.sku());

            if (productRepository.existsBySkuAndBranch(sku, branchId, tenantId)) {
                throw new DuplicateProductSkuException(sku.getValue());
            }

            Product product = Product.create(
                    tenantId,
                    branchId,
                    cmd.category(),
                    cmd.name(),
                    sku,
                    cmd.description(),
                    Money.of(cmd.salePrice()),
                    cmd.minimumStock() != null ? cmd.minimumStock() : 0
            );

            Product saved = productRepository.save(product);
            return toResult(saved);
        });
    }

    @Override
    public ProductResult getById(UUID productId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            ProductId pId = new ProductId(productId);

            Product product = productRepository.findById(pId, tId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            loadBatches(product, tId);
            return toResult(product);
        });
    }

    @Override
    public List<ProductResult> listByBranch(UUID branchId, UUID tenantId, String category, Boolean lowStockOnly) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            BranchId bId = new BranchId(branchId);

            List<Product> products;
            if (category != null && !category.trim().isEmpty()) {
                products = productRepository.findByBranchAndCategory(bId, category.trim(), tId);
            } else {
                products = productRepository.findByBranch(bId, tId);
            }

            return products.stream()
                    .filter(Product::isActive)
                    .peek(p -> loadBatches(p, tId))
                    .filter(p -> lowStockOnly == null || !lowStockOnly || p.isLowStockAlert())
                    .map(this::toResult)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public ProductResult update(UpdateProductCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            ProductId productId = new ProductId(cmd.productId());

            Product product = productRepository.findById(productId, tenantId)
                    .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));

            product.update(
                    cmd.category(),
                    cmd.name(),
                    cmd.description(),
                    Money.of(cmd.salePrice()),
                    cmd.minimumStock() != null ? cmd.minimumStock() : 0
            );

            loadBatches(product, tenantId);
            Product updated = productRepository.save(product);
            return toResult(updated);
        });
    }

    @Override
    public void delete(UUID productId, UUID tenantId) {
        unitOfWork.runInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            ProductId pId = new ProductId(productId);

            Product product = productRepository.findById(pId, tId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            loadBatches(product, tId);
            product.deactivate();
            productRepository.save(product);
        });
    }

    private void loadBatches(Product product, TenantId tenantId) {
        List<ProductBatch> batches = productBatchRepository.findByProductId(product.getId(), tenantId);
        batches.forEach(b -> {
            if (!product.getBatches().contains(b)) {
                product.addBatch(b);
            }
        });
    }

    private ProductResult toResult(Product p) {
        List<ProductBatchResult> batchResults = p.getBatches().stream()
                .map(this::toBatchResult)
                .collect(Collectors.toList());

        return new ProductResult(
                p.getId().getValue(),
                p.getBranchId().getValue(),
                p.getCategory(),
                p.getName(),
                p.getSku().getValue(),
                p.getDescription(),
                p.getSalePrice().getAmount(),
                p.getMinimumStock(),
                p.getCurrentStock(),
                p.isLowStockAlert(),
                p.isActive(),
                batchResults
        );
    }

    private ProductBatchResult toBatchResult(ProductBatch b) {
        return new ProductBatchResult(
                b.getId().getValue(),
                b.getBatchNumber(),
                b.getQuantity(),
                b.getRemainingQuantity(),
                b.getPurchasePrice().getAmount(),
                b.getExpirationDate(),
                b.getCreatedAt()
        );
    }
}
