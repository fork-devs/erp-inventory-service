package com.erp.inventory.application.service;

import com.erp.inventory.application.dto.command.AddProductBatchCommand;
import com.erp.inventory.application.dto.result.ProductBatchResult;
import com.erp.inventory.application.usecase.ProductBatchUseCase;
import com.erp.inventory.domain.exception.InvalidBatchException;
import com.erp.inventory.domain.exception.ProductNotFoundException;
import com.erp.inventory.domain.model.Product;
import com.erp.inventory.domain.model.ProductBatch;
import com.erp.inventory.domain.port.ProductBatchRepositoryPort;
import com.erp.inventory.domain.port.ProductRepositoryPort;
import com.erp.inventory.domain.port.UnitOfWork;
import com.erp.inventory.domain.valueobject.Money;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductBatchService implements ProductBatchUseCase {

    private final ProductRepositoryPort productRepository;
    private final ProductBatchRepositoryPort productBatchRepository;
    private final UnitOfWork unitOfWork;

    public ProductBatchService(ProductRepositoryPort productRepository,
                               ProductBatchRepositoryPort productBatchRepository,
                               UnitOfWork unitOfWork) {
        this.productRepository = productRepository;
        this.productBatchRepository = productBatchRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public ProductBatchResult addBatch(AddProductBatchCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            ProductId productId = new ProductId(cmd.productId());

            Product product = productRepository.findById(productId, tenantId)
                    .orElseThrow(() -> new ProductNotFoundException(cmd.productId()));

            if (productBatchRepository.findByProductIdAndBatchNumber(productId, cmd.batchNumber(), tenantId).isPresent()) {
                throw new InvalidBatchException("Batch number already exists for product: " + cmd.batchNumber());
            }

            ProductBatch batch = ProductBatch.create(
                    tenantId,
                    productId,
                    cmd.batchNumber(),
                    cmd.quantity(),
                    Money.of(cmd.purchasePrice()),
                    cmd.expirationDate()
            );

            ProductBatch savedBatch = productBatchRepository.save(batch);
            product.addBatch(savedBatch);
            productRepository.save(product);

            return toResult(savedBatch);
        });
    }

    @Override
    public List<ProductBatchResult> listBatches(UUID productId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            ProductId pId = new ProductId(productId);

            // Verify product exists
            productRepository.findById(pId, tId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));

            return productBatchRepository.findByProductId(pId, tId).stream()
                    .map(this::toResult)
                    .collect(Collectors.toList());
        });
    }

    private ProductBatchResult toResult(ProductBatch b) {
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
