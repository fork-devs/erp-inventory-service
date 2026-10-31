package com.erp.inventory.infrastructure.persistence.adapter;

import com.erp.inventory.domain.model.ProductBatch;
import com.erp.inventory.domain.port.ProductBatchRepositoryPort;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.mapper.ProductPersistenceMapper;
import com.erp.inventory.infrastructure.persistence.repository.SpringDataProductBatchRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProductBatchPersistenceAdapter implements ProductBatchRepositoryPort {

    private final SpringDataProductBatchRepository repo;

    public ProductBatchPersistenceAdapter(SpringDataProductBatchRepository repo) {
        this.repo = repo;
    }

    @Override
    public ProductBatch save(ProductBatch batch) {
        return ProductPersistenceMapper.toBatchDomain(
                repo.save(ProductPersistenceMapper.toBatchEntity(batch))
        );
    }

    @Override
    public List<ProductBatch> findByProductId(ProductId productId, TenantId tenantId) {
        return repo.findByProductIdAndTenantId(productId.getValue(), tenantId.getValue()).stream()
                .map(ProductPersistenceMapper::toBatchDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ProductBatch> findByProductIdAndBatchNumber(ProductId productId, String batchNumber, TenantId tenantId) {
        return repo.findByProductIdAndBatchNumberAndTenantId(productId.getValue(), batchNumber, tenantId.getValue())
                .map(ProductPersistenceMapper::toBatchDomain);
    }
}
