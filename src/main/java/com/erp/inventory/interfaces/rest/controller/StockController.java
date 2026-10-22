package com.erp.inventory.interfaces.rest.controller;

import com.erp.inventory.application.dto.command.RegisterStockMovementCommand;
import com.erp.inventory.application.dto.command.ReleaseStockCommand;
import com.erp.inventory.application.dto.command.ReserveStockCommand;
import com.erp.inventory.application.dto.result.StockBalanceResult;
import com.erp.inventory.application.dto.result.StockMovementResult;
import com.erp.inventory.application.usecase.StockOperationUseCase;
import com.erp.inventory.interfaces.rest.dto.request.RegisterStockMovementRequest;
import com.erp.inventory.interfaces.rest.dto.request.ReleaseStockRequest;
import com.erp.inventory.interfaces.rest.dto.request.ReserveStockRequest;
import com.erp.inventory.interfaces.rest.dto.response.StockBalanceResponse;
import com.erp.inventory.interfaces.rest.dto.response.StockMovementResponse;
import com.erp.inventory.interfaces.rest.support.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stock")
@Tag(name = "Stock Operations", description = "Operaciones de inventario, reservas y balance")
public class StockController {
    private final StockOperationUseCase stockUseCase;

    public StockController(StockOperationUseCase stockUseCase) {
        this.stockUseCase = stockUseCase;
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Registrar movimiento manual de stock (IN / OUT)")
    public ResponseEntity<StockMovementResponse> registerMovement(
            @Valid @RequestBody RegisterStockMovementRequest request) {
        StockMovementResult result = stockUseCase.registerMovement(
                new RegisterStockMovementCommand(CurrentTenant.id(), request.sparePartId(), request.branchId(),
                        request.movementType(), request.quantity(), request.unitCost(),
                        request.referenceId(), request.idempotencyKey()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/reserve")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Reservar stock para orden de trabajo (Saga)", description = "Operación idempotente mediante idempotencyKey")
    public ResponseEntity<StockMovementResponse> reserve(
            @Valid @RequestBody ReserveStockRequest request) {
        StockMovementResult result = stockUseCase.reserve(
                new ReserveStockCommand(CurrentTenant.id(), request.sparePartId(), request.branchId(),
                        request.quantity(), request.workOrderId(), request.idempotencyKey()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PostMapping("/commit")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Confirmar reserva de stock", description = "Consume el stock reservado de la orden de trabajo. Operacion idempotente")
    public ResponseEntity<StockMovementResponse> commit(
            @Valid @RequestBody ReserveStockRequest request) {
        StockMovementResult result = stockUseCase.commit(
                new ReserveStockCommand(CurrentTenant.id(), request.sparePartId(), request.branchId(),
                        request.quantity(), request.workOrderId(), request.idempotencyKey()));
        return ResponseEntity.status(HttpStatus.OK).body(toResponse(result));
    }

    @PostMapping("/release")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Liberar reserva de stock (Rollback Saga)", description = "Operación idempotente mediante idempotencyKey")
    public ResponseEntity<StockMovementResponse> release(
            @Valid @RequestBody ReleaseStockRequest request) {
        StockMovementResult result = stockUseCase.release(
                new ReleaseStockCommand(CurrentTenant.id(), request.sparePartId(), request.branchId(),
                        request.quantity(), request.workOrderId(), request.idempotencyKey()));
        return ResponseEntity.status(HttpStatus.OK).body(toResponse(result));
    }

    @GetMapping("/balance/{sparePartId}/branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    @Operation(summary = "Consultar saldo y stock disponible por sucursal")
    public ResponseEntity<StockBalanceResponse> getBalance(
            @PathVariable UUID sparePartId,
            @PathVariable UUID branchId) {
        StockBalanceResult result = stockUseCase.getBalance(sparePartId, branchId, CurrentTenant.id());
        return ResponseEntity.ok(new StockBalanceResponse(
                result.sparePartId(), result.branchId(), result.totalIn(),
                result.totalOut(), result.totalReserved(), result.availableStock(),
                result.availableValue(), result.reservedValue()));
    }

    private StockMovementResponse toResponse(StockMovementResult r) {
        return new StockMovementResponse(
                r.id(), r.tenantId(), r.sparePartId(), r.branchId(),
                r.movementType(), r.quantity(), r.referenceId(), r.idempotencyKey(), r.createdAt()
        );
    }
}
