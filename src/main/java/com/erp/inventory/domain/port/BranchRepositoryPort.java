package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.Branch;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface BranchRepositoryPort {
    Branch save(Branch branch);
    Optional<Branch> findById(BranchId id, TenantId tenantId);
    List<Branch> findAllByTenantId(TenantId tenantId);
    boolean existsById(BranchId id, TenantId tenantId);
}
