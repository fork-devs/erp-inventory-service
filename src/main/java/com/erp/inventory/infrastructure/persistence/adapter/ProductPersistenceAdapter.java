package com.erp.inventory.infrastructure.persistence.adapter;

import com.erp.inventory.domain.model.Product;
import com.erp.inventory.domain.port.ProductRepositoryPort;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.ProductId;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.mapper.ProductPersistenceMapper;
import com.erp.inventory.infrastructure.persistence.repository.SpringDataProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final SpringDataProductRepository repo;

    public ProductPersistenceAdapter(SpringDataProductRepository repo) {
        this.repo = repo;
    }

    @Override
    public Product save(Product product) {
        return ProductPersistenceMapper.toDomain(
                repo.save(ProductPersistenceMapper.toEntity(product))
        );
    }

    @Override
    public Optional<Product> findById(ProductId id, TenantId tenantId) {
        return repo.findByIdAndTenantId(id.getValue(), tenantId.getValue())
                .map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Product> findBySkuAndBranch(Sku sku, BranchId branchId, TenantId tenantId) {
        return repo.findByBranchIdAndSkuAndTenantId(branchId.getValue(), sku.getValue(), tenantId.getValue())
                .map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public List<Product> findByBranch(BranchId branchId, TenantId tenantId) {
        return repo.findByBranchIdAndTenantId(branchId.getValue(), tenantId.getValue()).stream()
                .map(ProductPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Product> findByBranchAndCategory(BranchId branchId, String category, TenantId tenantId) {
        return repo.findByBranchIdAndCategoryAndTenantId(branchId.getValue(), category, tenantId.getValue()).stream()
                .map(ProductPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySkuAndBranch(Sku sku, BranchId branchId, TenantId tenantId) {
        return repo.existsByBranchIdAndSkuAndTenantId(branchId.getValue(), sku.getValue(), tenantId.getValue());
    }
}
