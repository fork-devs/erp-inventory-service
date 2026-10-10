package com.erp.inventory.application.usecase;

import com.erp.inventory.application.dto.command.RegisterSparePartCommand;
import com.erp.inventory.application.dto.result.SparePartResult;

import java.util.List;
import java.util.UUID;

public interface SparePartUseCase {
    SparePartResult register(RegisterSparePartCommand command);
    SparePartResult getById(UUID sparePartId, UUID tenantId);
    List<SparePartResult> listAll(UUID tenantId);
}
