package com.erp.inventory.interfaces.rest.controller;

import com.erp.inventory.application.dto.command.AddProductBatchCommand;
import com.erp.inventory.application.dto.command.CreateProductCommand;
import com.erp.inventory.application.dto.command.UpdateProductCommand;
import com.erp.inventory.application.dto.result.ProductBatchResult;
import com.erp.inventory.application.dto.result.ProductResult;
import com.erp.inventory.application.usecase.ProductBatchUseCase;
import com.erp.inventory.application.usecase.ProductUseCase;
import com.erp.inventory.interfaces.rest.dto.request.AddProductBatchRequest;
import com.erp.inventory.interfaces.rest.dto.request.CreateProductRequest;
import com.erp.inventory.interfaces.rest.dto.request.UpdateProductRequest;
import com.erp.inventory.interfaces.rest.dto.response.ProductBatchResponse;
import com.erp.inventory.interfaces.rest.dto.response.ProductResponse;
import com.erp.inventory.interfaces.rest.support.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/inventory/products")
@Tag(name = "Products", description = "Management of branch products, batches, and stock")
public class ProductController {

    private final ProductUseCase productUseCase;
    private final ProductBatchUseCase productBatchUseCase;

    public ProductController(ProductUseCase productUseCase, ProductBatchUseCase productBatchUseCase) {
        this.productUseCase = productUseCase;
        this.productBatchUseCase = productBatchUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Create product in branch inventory")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        CreateProductCommand command = new CreateProductCommand(
                CurrentTenant.id(),
                request.branchId(),
                request.category(),
                request.name(),
                request.sku(),
                request.description(),
                request.salePrice(),
                request.minimumStock()
        );
        ProductResult result = productUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    @Operation(summary = "List branch products with optional category and low stock filters")
    public ResponseEntity<List<ProductResponse>> list(
            @RequestParam UUID branchId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean lowStockOnly) {
        List<ProductResult> results = productUseCase.listByBranch(branchId, CurrentTenant.id(), category, lowStockOnly);
        return ResponseEntity.ok(results.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    @Operation(summary = "Get product catalog for a specific branch")
    public ResponseEntity<List<ProductResponse>> listByBranch(@PathVariable UUID branchId) {
        List<ProductResult> results = productUseCase.listByBranch(branchId, CurrentTenant.id(), null, null);
        return ResponseEntity.ok(results.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'TECHNICIAN', 'MECANICO')")
    @Operation(summary = "Get product details with batches and stock")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID productId) {
        ProductResult result = productUseCase.getById(productId, CurrentTenant.id());
        return ResponseEntity.ok(toResponse(result));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Update product details")
    public ResponseEntity<ProductResponse> update(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {
        UpdateProductCommand command = new UpdateProductCommand(
                CurrentTenant.id(),
                productId,
                request.category(),
                request.name(),
                request.description(),
                request.salePrice(),
                request.minimumStock()
        );
        ProductResult result = productUseCase.update(command);
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{productId}/batches")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Add stock batch to product")
    public ResponseEntity<ProductBatchResponse> addBatch(
            @PathVariable UUID productId,
            @Valid @RequestBody AddProductBatchRequest request) {
        AddProductBatchCommand command = new AddProductBatchCommand(
                CurrentTenant.id(),
                productId,
                request.batchNumber(),
                request.quantity(),
                request.purchasePrice(),
                request.expirationDate()
        );
        ProductBatchResult result = productBatchUseCase.addBatch(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toBatchResponse(result));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Delete product (must have zero stock)")
    public ResponseEntity<Void> delete(@PathVariable UUID productId) {
        productUseCase.delete(productId, CurrentTenant.id());
        return ResponseEntity.noContent().build();
    }

    private ProductResponse toResponse(ProductResult r) {
        List<ProductBatchResponse> batchResponses = r.batches() != null
                ? r.batches().stream().map(this::toBatchResponse).collect(Collectors.toList())
                : List.of();

        return new ProductResponse(
                r.id(),
                r.branchId(),
                r.category(),
                r.name(),
                r.sku(),
                r.description(),
                r.salePrice(),
                r.minimumStock(),
                r.currentStock(),
                r.lowStockAlert(),
                batchResponses
        );
    }

    private ProductBatchResponse toBatchResponse(ProductBatchResult b) {
        return new ProductBatchResponse(
                b.id(),
                b.batchNumber(),
                b.quantity(),
                b.remainingQuantity(),
                b.purchasePrice(),
                b.expirationDate(),
                b.createdAt()
        );
    }
}
