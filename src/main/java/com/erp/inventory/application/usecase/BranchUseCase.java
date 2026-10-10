package com.erp.inventory.application.usecase;

import com.erp.inventory.application.dto.command.RegisterBranchCommand;
import com.erp.inventory.application.dto.result.BranchResult;

import java.util.List;
import java.util.UUID;

public interface BranchUseCase {
    BranchResult register(RegisterBranchCommand command);
    BranchResult getById(UUID branchId, UUID tenantId);
    List<BranchResult> listAll(UUID tenantId);
}
