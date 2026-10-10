package com.erp.inventory.interfaces.rest.controller;

import com.erp.inventory.application.dto.command.RegisterSparePartCommand;
import com.erp.inventory.application.dto.result.SparePartResult;
import com.erp.inventory.application.usecase.SparePartUseCase;
import com.erp.inventory.interfaces.rest.dto.request.RegisterSparePartRequest;
import com.erp.inventory.interfaces.rest.dto.response.SparePartResponse;
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
@RequestMapping("/api/v1/spare-parts")
@Tag(name = "Spare Parts", description = "Gestión del catálogo de repuestos")
public class SparePartController {
    private final SparePartUseCase sparePartUseCase;

    public SparePartController(SparePartUseCase sparePartUseCase) {
        this.sparePartUseCase = sparePartUseCase;
    }

    @PostMapping
    @Operation(summary = "Registrar repuesto en catálogo")
    public ResponseEntity<SparePartResponse> register(
            @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Valid @RequestBody RegisterSparePartRequest request) {
        SparePartResult result = sparePartUseCase.register(
                new RegisterSparePartCommand(tenantId, request.sku(), request.description(), request.minStock()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping
    @Operation(summary = "Listar repuestos del taller")
    public ResponseEntity<List<SparePartResponse>> listAll(@RequestHeader("X-Tenant-ID") UUID tenantId) {
        return ResponseEntity.ok(sparePartUseCase.listAll(tenantId).stream()
                .map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener repuesto por ID")
    public ResponseEntity<SparePartResponse> getById(
            @RequestHeader("X-Tenant-ID") UUID tenantId, @PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(sparePartUseCase.getById(id, tenantId)));
    }

    private SparePartResponse toResponse(SparePartResult r) {
        return new SparePartResponse(r.id(), r.tenantId(), r.sku(), r.description(), r.minStock(), r.createdAt());
    }
}
