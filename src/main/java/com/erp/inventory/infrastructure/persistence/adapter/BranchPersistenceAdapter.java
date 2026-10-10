package com.erp.inventory.infrastructure.persistence.adapter;

import com.erp.inventory.domain.model.Branch;
import com.erp.inventory.domain.port.BranchRepositoryPort;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.TenantId;
import com.erp.inventory.infrastructure.persistence.mapper.BranchPersistenceMapper;
import com.erp.inventory.infrastructure.persistence.repository.SpringDataBranchRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class BranchPersistenceAdapter implements BranchRepositoryPort {
    private final SpringDataBranchRepository repo;

    public BranchPersistenceAdapter(SpringDataBranchRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public Branch save(Branch branch) {
        return BranchPersistenceMapper.toDomain(repo.save(BranchPersistenceMapper.toEntity(branch)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Branch> findById(BranchId id, TenantId tenantId) {
        return repo.findByIdAndTenantId(id.getValue(), tenantId.getValue())
                .map(BranchPersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Branch> findAllByTenantId(TenantId tenantId) {
        return repo.findAllByTenantId(tenantId.getValue()).stream()
                .map(BranchPersistenceMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(BranchId id, TenantId tenantId) {
        return repo.existsByIdAndTenantId(id.getValue(), tenantId.getValue());
    }
}
