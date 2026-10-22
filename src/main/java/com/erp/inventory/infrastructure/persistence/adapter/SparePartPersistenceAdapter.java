package com.erp.inventory.infrastructure.persistence.adapter;

import com.erp.inventory.domain.model.SparePart;
import com.erp.inventory.domain.port.SparePartRepositoryPort;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.mapper.SparePartPersistenceMapper;
import com.erp.inventory.infrastructure.persistence.repository.SpringDataSparePartRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SparePartPersistenceAdapter implements SparePartRepositoryPort {
    private final SpringDataSparePartRepository repo;

    public SparePartPersistenceAdapter(SpringDataSparePartRepository repo) {
        this.repo = repo;
    }

    @Override
    public SparePart save(SparePart sparePart) {
        return SparePartPersistenceMapper.toDomain(repo.save(SparePartPersistenceMapper.toEntity(sparePart)));
    }

    @Override
    public Optional<SparePart> findById(SparePartId id, TenantId tenantId) {
        return repo.findByIdAndTenantId(id.getValue(), tenantId.getValue())
                .map(SparePartPersistenceMapper::toDomain);
    }

    @Override
    public List<SparePart> findAllByTenantId(TenantId tenantId) {
        return repo.findAllByTenantId(tenantId.getValue()).stream()
                .map(SparePartPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsBySku(String sku, TenantId tenantId) {
        return repo.existsBySkuAndTenantId(sku, tenantId.getValue());
    }

    @Override
    public boolean existsById(SparePartId id, TenantId tenantId) {
        return repo.existsByIdAndTenantId(id.getValue(), tenantId.getValue());
    }
}
