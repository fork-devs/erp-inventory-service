package com.erp.inventory.application.service;

import com.erp.inventory.application.dto.command.RegisterBranchCommand;
import com.erp.inventory.application.dto.result.BranchResult;
import com.erp.inventory.application.usecase.BranchUseCase;
import com.erp.inventory.domain.exception.BranchNotFoundException;
import com.erp.inventory.domain.model.Branch;
import com.erp.inventory.domain.port.BranchRepositoryPort;
import com.erp.inventory.domain.port.UnitOfWork;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BranchService implements BranchUseCase {

    private final BranchRepositoryPort branchRepository;
    private final UnitOfWork unitOfWork;

    public BranchService(BranchRepositoryPort branchRepository, UnitOfWork unitOfWork) {
        this.branchRepository = branchRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public BranchResult register(RegisterBranchCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            Branch branch = Branch.create(tenantId, cmd.name(), cmd.address());
            return toResult(branchRepository.save(branch));
        });
    }

    @Override
    public BranchResult getById(UUID branchId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> branchRepository
                .findById(new BranchId(branchId), new TenantId(tenantId))
                .map(this::toResult)
                .orElseThrow(() -> new BranchNotFoundException(branchId.toString())));
    }

    @Override
    public List<BranchResult> listAll(UUID tenantId) {
        return unitOfWork.callInTransaction(() -> branchRepository
                .findAllByTenantId(new TenantId(tenantId))
                .stream().map(this::toResult).collect(Collectors.toList()));
    }

    private BranchResult toResult(Branch b) {
        return new BranchResult(b.getId().getValue(), b.getTenantId().getValue(),
                b.getName(), b.getAddress(), b.getCreatedAt());
    }
}
