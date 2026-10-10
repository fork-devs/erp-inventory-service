package com.erp.inventory.interfaces.rest.controller;

import com.erp.inventory.application.dto.command.RegisterBranchCommand;
import com.erp.inventory.application.dto.result.BranchResult;
import com.erp.inventory.application.usecase.BranchUseCase;
import com.erp.inventory.interfaces.rest.dto.request.RegisterBranchRequest;
import com.erp.inventory.interfaces.rest.dto.response.BranchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/branches")
@Tag(name = "Branches", description = "Gestión de sucursales y almacenes del taller")
public class BranchController {
    private final BranchUseCase branchUseCase;

    public BranchController(BranchUseCase branchUseCase) {
        this.branchUseCase = branchUseCase;
    }

    @PostMapping
    @Operation(summary = "Registrar sucursal o almacén")
    public ResponseEntity<BranchResponse> register(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Valid @RequestBody RegisterBranchRequest request) {
        BranchResult result = branchUseCase.register(
                new RegisterBranchCommand(tenantId, request.name(), request.address()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping
    @Operation(summary = "Listar sucursales del taller")
    public ResponseEntity<List<BranchResponse>> listAll(@RequestHeader("X-Tenant-ID") UUID tenantId) {
        return ResponseEntity.ok(branchUseCase.listAll(tenantId).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener sucursal por ID")
    public ResponseEntity<BranchResponse> getById(
            @RequestHeader("X-Tenant-ID") UUID tenantId, @PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(branchUseCase.getById(id, tenantId)));
    }

    private BranchResponse toResponse(BranchResult r) {
        return new BranchResponse(r.id(), r.tenantId(), r.name(), r.address(), r.createdAt());
    }
}
