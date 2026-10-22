package com.erp.inventory.application.service;

import com.erp.inventory.application.dto.command.RegisterSparePartCommand;
import com.erp.inventory.application.dto.result.SparePartResult;
import com.erp.inventory.application.usecase.SparePartUseCase;
import com.erp.inventory.domain.exception.SkuAlreadyExistsException;
import com.erp.inventory.domain.exception.SparePartNotFoundException;
import com.erp.inventory.domain.model.SparePart;
import com.erp.inventory.domain.port.SparePartRepositoryPort;
import com.erp.inventory.domain.port.UnitOfWork;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SparePartService implements SparePartUseCase {

    private final SparePartRepositoryPort sparePartRepository;
    private final UnitOfWork unitOfWork;

    public SparePartService(SparePartRepositoryPort sparePartRepository, UnitOfWork unitOfWork) {
        this.sparePartRepository = sparePartRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public SparePartResult register(RegisterSparePartCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            Sku sku = Sku.of(cmd.sku());
            if (sparePartRepository.existsBySku(sku.getValue(), tenantId)) {
                throw new SkuAlreadyExistsException(sku.getValue());
            }
            SparePart part = SparePart.create(tenantId, sku, cmd.description(), cmd.minStock());
            return toResult(sparePartRepository.save(part));
        });
    }

    @Override
    public SparePartResult getById(UUID sparePartId, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> sparePartRepository
                .findById(new SparePartId(sparePartId), new TenantId(tenantId))
                .map(this::toResult)
                .orElseThrow(() -> new SparePartNotFoundException(sparePartId.toString())));
    }

    @Override
    public List<SparePartResult> listAll(UUID tenantId) {
        return unitOfWork.callInTransaction(() -> sparePartRepository
                .findAllByTenantId(new TenantId(tenantId))
                .stream().map(this::toResult).collect(Collectors.toList()));
    }

    private SparePartResult toResult(SparePart p) {
        return new SparePartResult(p.getId().getValue(), p.getTenantId().getValue(),
                p.getSku().getValue(), p.getDescription(), p.getMinStock(), p.getCreatedAt());
    }
}
