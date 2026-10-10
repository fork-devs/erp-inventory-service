package com.erp.inventory.infrastructure.persistence.adapter;

import com.erp.inventory.domain.model.StockMovement;
import com.erp.inventory.domain.port.StockMovementRepositoryPort;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.mapper.StockMovementPersistenceMapper;
import com.erp.inventory.infrastructure.persistence.repository.SpringDataStockMovementRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class StockMovementPersistenceAdapter implements StockMovementRepositoryPort {
    private final SpringDataStockMovementRepository repo;

    public StockMovementPersistenceAdapter(SpringDataStockMovementRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public StockMovement save(StockMovement movement) {
        return StockMovementPersistenceMapper.toDomain(repo.save(StockMovementPersistenceMapper.toEntity(movement)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovement> findBySparePartAndBranch(SparePartId sparePartId, BranchId branchId, TenantId tenantId) {
        return repo.findBySparePartIdAndBranchIdAndTenantId(sparePartId.getValue(), branchId.getValue(), tenantId.getValue())
                .stream().map(StockMovementPersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovement> findBySparePart(SparePartId sparePartId, TenantId tenantId) {
        return repo.findBySparePartIdAndTenantId(sparePartId.getValue(), tenantId.getValue())
                .stream().map(StockMovementPersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockMovement> findByIdempotencyKey(String idempotencyKey, TenantId tenantId) {
        return repo.findByIdempotencyKeyAndTenantId(idempotencyKey, tenantId.getValue())
                .map(StockMovementPersistenceMapper::toDomain);
    }
}
