package com.erp.inventory.application.service;

import com.erp.inventory.application.dto.command.RegisterStockMovementCommand;
import com.erp.inventory.application.dto.command.ReleaseStockCommand;
import com.erp.inventory.application.dto.command.ReserveStockCommand;
import com.erp.inventory.application.dto.result.StockBalanceResult;
import com.erp.inventory.application.dto.result.StockMovementResult;
import com.erp.inventory.application.usecase.StockOperationUseCase;
import com.erp.inventory.domain.exception.BranchNotFoundException;
import com.erp.inventory.domain.exception.InsufficientStockException;
import com.erp.inventory.domain.exception.SparePartNotFoundException;
import com.erp.inventory.domain.model.MovementType;
import com.erp.inventory.domain.model.StockMovement;
import com.erp.inventory.domain.port.BranchRepositoryPort;
import com.erp.inventory.domain.port.SparePartRepositoryPort;
import com.erp.inventory.domain.port.StockMovementRepositoryPort;
import com.erp.inventory.domain.port.UnitOfWork;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.IdempotencyKey;
import com.erp.inventory.domain.valueobject.Quantity;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StockOperationService implements StockOperationUseCase {

    private final StockMovementRepositoryPort movementRepository;
    private final SparePartRepositoryPort sparePartRepository;
    private final BranchRepositoryPort branchRepository;
    private final UnitOfWork unitOfWork;

    public StockOperationService(StockMovementRepositoryPort movementRepository,
                                  SparePartRepositoryPort sparePartRepository,
                                  BranchRepositoryPort branchRepository,
                                  UnitOfWork unitOfWork) {
        this.movementRepository = movementRepository;
        this.sparePartRepository = sparePartRepository;
        this.branchRepository = branchRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public StockMovementResult registerMovement(RegisterStockMovementCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            SparePartId partId = new SparePartId(cmd.sparePartId());
            BranchId branchId = new BranchId(cmd.branchId());
            validateEntitiesExist(partId, branchId, tenantId);
            MovementType type = MovementType.valueOf(cmd.movementType().toUpperCase());
            Quantity qty = Quantity.of(cmd.quantity());
            if (type == MovementType.OUT) {
                StockBalanceResult balance = getBalance(cmd.sparePartId(), cmd.branchId(), cmd.tenantId());
                if (balance.availableStock() < qty.getValue()) {
                    throw new InsufficientStockException("Stock insuficiente para salida. Disponible: " +
                            balance.availableStock() + ", solicitado: " + qty.getValue());
                }
            }
            IdempotencyKey idempKey = cmd.idempotencyKey() != null ? IdempotencyKey.of(cmd.idempotencyKey()) : null;
            if (idempKey != null) {
                Optional<StockMovement> existing = movementRepository.findByIdempotencyKey(idempKey.getValue(), tenantId);
                if (existing.isPresent()) {
                    return toResult(existing.get());
                }
            }
            StockMovement movement = StockMovement.create(tenantId, partId, branchId, type, qty,
                    cmd.referenceId(), idempKey);
            return toResult(movementRepository.save(movement));
        });
    }

    @Override
    public StockMovementResult reserve(ReserveStockCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            SparePartId partId = new SparePartId(cmd.sparePartId());
            BranchId branchId = new BranchId(cmd.branchId());
            validateEntitiesExist(partId, branchId, tenantId);
            // Idempotencia: si ya se ejecuto con la misma clave para esta saga,
            // se retorna el registro existente.
            if (cmd.idempotencyKey() != null) {
                Optional<StockMovement> existing = movementRepository.findByIdempotencyKey(cmd.idempotencyKey(), tenantId);
                if (existing.isPresent()) {
                    return toResult(existing.get());
                }
            }
            StockBalanceResult balance = getBalance(cmd.sparePartId(), cmd.branchId(), cmd.tenantId());
            if (balance.availableStock() < cmd.quantity()) {
                throw new InsufficientStockException("No hay suficiente stock disponible para reservar. Disponible: " +
                        balance.availableStock() + ", Requerido: " + cmd.quantity());
            }
            StockMovement movement = StockMovement.create(
                    tenantId, partId, branchId, MovementType.RESERVE, Quantity.of(cmd.quantity()),
                    cmd.workOrderId(), cmd.idempotencyKey() != null ? IdempotencyKey.of(cmd.idempotencyKey()) : null
            );
            return toResult(movementRepository.save(movement));
        });
    }

    @Override
    public StockMovementResult release(ReleaseStockCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            SparePartId partId = new SparePartId(cmd.sparePartId());
            BranchId branchId = new BranchId(cmd.branchId());
            validateEntitiesExist(partId, branchId, tenantId);
            if (cmd.idempotencyKey() != null) {
                Optional<StockMovement> existing = movementRepository.findByIdempotencyKey(cmd.idempotencyKey(), tenantId);
                if (existing.isPresent()) {
                    return toResult(existing.get());
                }
            }
            StockMovement movement = StockMovement.create(
                    tenantId, partId, branchId, MovementType.RELEASE, Quantity.of(cmd.quantity()),
                    cmd.workOrderId(), cmd.idempotencyKey() != null ? IdempotencyKey.of(cmd.idempotencyKey()) : null
            );
            return toResult(movementRepository.save(movement));
        });
    }

    @Override
    public StockBalanceResult getBalance(UUID sparePartId, UUID branchId, UUID tenantId) {
        TenantId tId = new TenantId(tenantId);
        SparePartId pId = new SparePartId(sparePartId);
        BranchId bId = new BranchId(branchId);

        List<StockMovement> movements = movementRepository.findBySparePartAndBranch(pId, bId, tId);

        int totalIn = 0;
        int totalOut = 0;
        int totalReserved = 0;
        int totalReleased = 0;

        for (StockMovement m : movements) {
            switch (m.getMovementType()) {
                case IN -> totalIn += m.getQuantity().getValue();
                case OUT -> totalOut += m.getQuantity().getValue();
                case RESERVE -> totalReserved += m.getQuantity().getValue();
                case RELEASE -> totalReleased += m.getQuantity().getValue();
                case COMMIT -> totalOut += m.getQuantity().getValue();
            }
        }

        int activeReserved = Math.max(0, totalReserved - totalReleased);
        int available = totalIn - totalOut - activeReserved;

        return new StockBalanceResult(sparePartId, branchId, totalIn, totalOut, activeReserved, Math.max(0, available));
    }

    private void validateEntitiesExist(SparePartId partId, BranchId branchId, TenantId tenantId) {
        if (!sparePartRepository.existsById(partId, tenantId)) {
            throw new SparePartNotFoundException(partId.getValue().toString());
        }
        if (!branchRepository.existsById(branchId, tenantId)) {
            throw new BranchNotFoundException(branchId.getValue().toString());
        }
    }

    private StockMovementResult toResult(StockMovement m) {
        return new StockMovementResult(
                m.getId().getValue(), m.getTenantId().getValue(),
                m.getSparePartId().getValue(), m.getBranchId().getValue(),
                m.getMovementType().name(), m.getQuantity().getValue(),
                m.getReferenceId(), m.getIdempotencyKey() == null ? null : m.getIdempotencyKey().getValue(),
                m.getCreatedAt()
        );
    }
}
