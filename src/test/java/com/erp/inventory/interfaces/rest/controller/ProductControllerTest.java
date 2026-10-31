package com.erp.inventory.interfaces.rest.controller;

import com.erp.inventory.application.dto.command.AddProductBatchCommand;
import com.erp.inventory.application.dto.command.CreateProductCommand;
import com.erp.inventory.application.dto.command.UpdateProductCommand;
import com.erp.inventory.application.dto.result.ProductBatchResult;
import com.erp.inventory.application.dto.result.ProductResult;
import com.erp.inventory.application.usecase.ProductBatchUseCase;
import com.erp.inventory.application.usecase.ProductUseCase;
import com.erp.inventory.domain.exception.ActiveStockConflictException;
import com.erp.inventory.domain.exception.DuplicateProductSkuException;
import com.erp.inventory.domain.exception.ProductNotFoundException;
import com.erp.inventory.infrastructure.persistence.rls.TenantContextHolder;
import com.erp.inventory.interfaces.rest.advice.GlobalRestExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductUseCase productUseCase;

    @Mock
    private ProductBatchUseCase productBatchUseCase;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(tenantId);
        ProductController controller = new ProductController(productUseCase, productBatchUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalRestExceptionHandler(null))
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("POST /api/v1/inventory/products should create product and return 201 Created")
    void shouldCreateProduct() throws Exception {
        ProductResult result = new ProductResult(
                productId,
                branchId,
                "Filters",
                "Oil Filter Premium",
                "FLT-001",
                "High performance filter",
                BigDecimal.valueOf(45.0),
                5,
                0,
                true,
                true,
                List.of()
        );

        when(productUseCase.create(any(CreateProductCommand.class))).thenReturn(result);

        String payload = """
                {
                    "branchId": "%s",
                    "category": "Filters",
                    "name": "Oil Filter Premium",
                    "sku": "FLT-001",
                    "description": "High performance filter",
                    "salePrice": 45.00,
                    "minimumStock": 5
                }
                """.formatted(branchId);

        mockMvc.perform(post("/api/v1/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.category").value("Filters"))
                .andExpect(jsonPath("$.name").value("Oil Filter Premium"))
                .andExpect(jsonPath("$.sku").value("FLT-001"))
                .andExpect(jsonPath("$.currentStock").value(0))
                .andExpect(jsonPath("$.lowStockAlert").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/products should return 400 when duplicate SKU occurs")
    void shouldReturn400OnDuplicateSku() throws Exception {
        when(productUseCase.create(any(CreateProductCommand.class)))
                .thenThrow(new DuplicateProductSkuException("FLT-001"));

        String payload = """
                {
                    "branchId": "%s",
                    "category": "Filters",
                    "name": "Oil Filter Premium",
                    "sku": "FLT-001",
                    "description": "Desc",
                    "salePrice": 45.00,
                    "minimumStock": 5
                }
                """.formatted(branchId);

        mockMvc.perform(post("/api/v1/inventory/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/products?branchId={id} should return products list")
    void shouldListProductsByBranch() throws Exception {
        ProductResult p = new ProductResult(
                productId,
                branchId,
                "Filters",
                "Air Filter",
                "AF-001",
                "Desc",
                BigDecimal.valueOf(30.0),
                2,
                10,
                false,
                true,
                List.of()
        );

        when(productUseCase.listByBranch(eq(branchId), eq(tenantId), isNull(), isNull()))
                .thenReturn(List.of(p));

        mockMvc.perform(get("/api/v1/inventory/products")
                        .param("branchId", branchId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId.toString()))
                .andExpect(jsonPath("$[0].currentStock").value(10))
                .andExpect(jsonPath("$[0].lowStockAlert").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/products/branch/{branchId} should return branch catalog")
    void shouldGetCatalogByBranch() throws Exception {
        ProductResult p = new ProductResult(
                productId,
                branchId,
                "Filters",
                "Cabin Filter",
                "CF-001",
                "Desc",
                BigDecimal.valueOf(25.0),
                3,
                1,
                true,
                true,
                List.of()
        );

        when(productUseCase.listByBranch(eq(branchId), eq(tenantId), isNull(), isNull()))
                .thenReturn(List.of(p));

        mockMvc.perform(get("/api/v1/inventory/products/branch/" + branchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cabin Filter"))
                .andExpect(jsonPath("$[0].lowStockAlert").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/products/{productId} should return product details with batches")
    void shouldGetProductDetails() throws Exception {
        ProductBatchResult b = new ProductBatchResult(
                UUID.randomUUID(),
                "LOT-01",
                15,
                12,
                BigDecimal.valueOf(18.0),
                null,
                Instant.now()
        );

        ProductResult p = new ProductResult(
                productId,
                branchId,
                "Brakes",
                "Brake Rotor",
                "BRK-R1",
                "Desc",
                BigDecimal.valueOf(180.0),
                4,
                12,
                false,
                true,
                List.of(b)
        );

        when(productUseCase.getById(eq(productId), eq(tenantId))).thenReturn(p);

        mockMvc.perform(get("/api/v1/inventory/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.batches[0].batchNumber").value("LOT-01"))
                .andExpect(jsonPath("$.batches[0].remainingQuantity").value(12));
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/products/{productId} should update and return 200")
    void shouldUpdateProduct() throws Exception {
        ProductResult updated = new ProductResult(
                productId,
                branchId,
                "Brakes",
                "Updated Rotor",
                "BRK-R1",
                "New description",
                BigDecimal.valueOf(195.0),
                6,
                0,
                true,
                true,
                List.of()
        );

        when(productUseCase.update(any(UpdateProductCommand.class))).thenReturn(updated);

        String payload = """
                {
                    "category": "Brakes",
                    "name": "Updated Rotor",
                    "description": "New description",
                    "salePrice": 195.00,
                    "minimumStock": 6
                }
                """;

        mockMvc.perform(put("/api/v1/inventory/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Rotor"))
                .andExpect(jsonPath("$.salePrice").value(195.0));
    }

    @Test
    @DisplayName("POST /api/v1/inventory/products/{productId}/batches should add batch and return 201")
    void shouldAddBatch() throws Exception {
        UUID batchId = UUID.randomUUID();
        ProductBatchResult b = new ProductBatchResult(
                batchId,
                "BATCH-2026",
                50,
                50,
                BigDecimal.valueOf(12.5),
                null,
                Instant.now()
        );

        when(productBatchUseCase.addBatch(any(AddProductBatchCommand.class))).thenReturn(b);

        String payload = """
                {
                    "batchNumber": "BATCH-2026",
                    "quantity": 50,
                    "purchasePrice": 12.50
                }
                """;

        mockMvc.perform(post("/api/v1/inventory/products/" + productId + "/batches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.batchNumber").value("BATCH-2026"))
                .andExpect(jsonPath("$.quantity").value(50));
    }

    @Test
    @DisplayName("DELETE /api/v1/inventory/products/{productId} should return 204 when stock is zero")
    void shouldDeleteProduct() throws Exception {
        doNothing().when(productUseCase).delete(eq(productId), eq(tenantId));

        mockMvc.perform(delete("/api/v1/inventory/products/" + productId))
                .andExpect(status().isNoContent());

        verify(productUseCase).delete(eq(productId), eq(tenantId));
    }

    @Test
    @DisplayName("DELETE /api/v1/inventory/products/{productId} should return 409 Conflict when stock remains")
    void shouldReturn409WhenStockRemains() throws Exception {
        doThrow(new ActiveStockConflictException(productId, 15))
                .when(productUseCase).delete(eq(productId), eq(tenantId));

        mockMvc.perform(delete("/api/v1/inventory/products/" + productId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_HAS_STOCK"));
    }
}
