package com.erp.inventory.infrastructure.persistence.mapper;

import com.erp.inventory.domain.model.Product;
import com.erp.inventory.domain.model.ProductBatch;
import com.erp.inventory.domain.valueobject.BatchId;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.Money;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.entity.ProductBatchJpaEntity;
import com.erp.inventory.infrastructure.persistence.entity.ProductJpaEntity;

import java.util.ArrayList;

public class ProductPersistenceMapper {
    private ProductPersistenceMapper() {}

    public static ProductJpaEntity toEntity(Product p) {
        return new ProductJpaEntity(
                p.getId().getValue(),
                p.getTenantId().getValue(),
                p.getBranchId().getValue(),
                p.getCategory(),
                p.getName(),
                p.getSku().getValue(),
                p.getDescription(),
                p.getSalePrice().getAmount(),
                p.getMinimumStock(),
                p.isActive(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    public static Product toDomain(ProductJpaEntity e) {
        return new Product(
                new ProductId(e.getId()),
                new TenantId(e.getTenantId()),
                new BranchId(e.getBranchId()),
                e.getCategory(),
                e.getName(),
                Sku.of(e.getSku()),
                e.getDescription(),
                Money.of(e.getSalePrice()),
                e.getMinimumStock(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                new ArrayList<>()
        );
    }

    public static ProductBatchJpaEntity toBatchEntity(ProductBatch b) {
        return new ProductBatchJpaEntity(
                b.getId().getValue(),
                b.getTenantId().getValue(),
                b.getProductId().getValue(),
                b.getBatchNumber(),
                b.getQuantity(),
                b.getRemainingQuantity(),
                b.getPurchasePrice().getAmount(),
                b.getExpirationDate(),
                b.getCreatedAt()
        );
    }

    public static ProductBatch toBatchDomain(ProductBatchJpaEntity e) {
        return new ProductBatch(
                new BatchId(e.getId()),
                new TenantId(e.getTenantId()),
                new ProductId(e.getProductId()),
                e.getBatchNumber(),
                e.getQuantity(),
                e.getRemainingQuantity(),
                Money.of(e.getPurchasePrice()),
                e.getExpirationDate(),
                e.getCreatedAt()
        );
    }
}
