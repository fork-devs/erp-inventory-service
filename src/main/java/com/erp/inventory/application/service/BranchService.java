package com.erp.inventory.application.service;

import com.erp.inventory.application.dto.command.RegisterBranchCommand;
import com.erp.inventory.application.dto.result.BranchResult;
import com.erp.inventory.application.usecase.BranchUseCase;
import com.erp.inventory.domain.exception.BranchNotFoundException;
import com.erp.inventory.domain.model.Branch;
import com.erp.inventory.domain.port.BranchRepositoryPort;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BranchService implements BranchUseCase {
    private final BranchRepositoryPort branchRepository;

    public BranchService(BranchRepositoryPort branchRepository) {
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional
    public BranchResult register(RegisterBranchCommand cmd) {
        TenantId tenantId = new TenantId(cmd.tenantId());
        Branch branch = Branch.create(tenantId, cmd.name(), cmd.address());
        return toResult(branchRepository.save(branch));
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResult getById(UUID branchId, UUID tenantId) {
        return branchRepository.findById(new BranchId(branchId), new TenantId(tenantId))
                .map(this::toResult)
                .orElseThrow(() -> new BranchNotFoundException(branchId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResult> listAll(UUID tenantId) {
        return branchRepository.findAllByTenantId(new TenantId(tenantId))
                .stream().map(this::toResult).collect(Collectors.toList());
    }

    private BranchResult toResult(Branch b) {
        return new BranchResult(b.getId().getValue(), b.getTenantId().getValue(), b.getName(), b.getAddress(), b.getCreatedAt());
    }
}
