package com.erp.inventory.support;

import com.erp.inventory.application.dto.command.RegisterBranchCommand;
import com.erp.inventory.application.dto.command.RegisterSparePartCommand;
import com.erp.inventory.application.dto.result.BranchResult;
import com.erp.inventory.application.dto.result.SparePartResult;
import com.erp.inventory.application.usecase.BranchUseCase;
import com.erp.inventory.application.usecase.SparePartUseCase;
import com.erp.inventory.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract tests verifying REST API endpoint routes, request validation, and ProblemDetail structures.
 *
 * <p>Endpoints are verified with gateway route prefixes, machine-readable error codes,
 * and tenant context propagation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InventoryApiContractTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BranchUseCase branchUseCase;

    @Autowired
    private SparePartUseCase sparePartUseCase;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("El alta de una sucursal responde 201")
    void elAltaDeUnaSucursalResponde201() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/branches")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sucursal de prueba\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()));
    }

    @Test
    @DisplayName("Un SKU repetido responde 409 con su codigo estable")
    void unSkuRepetidoResponde409ConSuCodigoEstable() throws Exception {
        String sku = sku();
        crearRepuesto(sku);

        mockMvc.perform(post("/api/v1/inventory/spare-parts")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"" + sku + "\",\"description\":\"Otro\",\"minStock\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVENTORY_SKU_DUPLICADO"))
                .andExpect(jsonPath("$.tenant_id").value(tenantId.toString()));
    }

    @Test
    @DisplayName("Una reserva sin stock suficiente responde 409 con su codigo")
    void unaReservaSinStockSuficienteResponde409ConSuCodigo() throws Exception {
        UUID repuesto = crearRepuesto(sku());
        UUID sucursal = crearSucursal();

        mockMvc.perform(post("/api/v1/inventory/stock/reserve")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sparePartId\":\"" + repuesto + "\",\"branchId\":\"" + sucursal
                                + "\",\"quantity\":5,\"idempotencyKey\":\"reserva-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVENTORY_INSUFFICIENT_STOCK"));
    }

    @Test
    @DisplayName("Un registro de otro taller responde 404")
    void unRegistroDeOtroTallerResponde404() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/spare-parts/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVENTORY_PART_NOT_FOUND"));
    }

    @Test
    @DisplayName("Un rol sin permiso responde 403")
    void unRolSinPermisoResponde403() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/branches")
                        .header("Authorization", "Bearer "
                                + TestJwksServer.signToken(tenantId, List.of("MECANICO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sucursal\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Una peticion sin token responde 401")
    void unaPeticionSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/spare-parts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La ruta sin el prefijo del gateway no existe")
    void laRutaSinElPrefijoDelGatewayNoExiste() throws Exception {
        mockMvc.perform(get("/api/v1/spare-parts")
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La confirmacion de una reserva responde 200")
    void laConfirmacionDeUnaReservaResponde200() throws Exception {
        UUID repuesto = crearRepuesto(sku());
        UUID sucursal = crearSucursal();

        mockMvc.perform(post("/api/v1/inventory/stock/movements")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sparePartId\":\"" + repuesto + "\",\"branchId\":\"" + sucursal
                                + "\",\"movementType\":\"IN\",\"quantity\":10,\"unitCost\":25.00,"
                                + "\"idempotencyKey\":\"entrada-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/inventory/stock/reserve")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sparePartId\":\"" + repuesto + "\",\"branchId\":\"" + sucursal
                                + "\",\"quantity\":3,\"idempotencyKey\":\"reserva-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/inventory/stock/commit")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sparePartId\":\"" + repuesto + "\",\"branchId\":\"" + sucursal
                                + "\",\"quantity\":3,\"idempotencyKey\":\"commit-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementType").value("COMMIT"));
    }

    @Test
    @DisplayName("El saldo informa del valor del stock")
    void elSaldoInformaDelValorDelStock() throws Exception {
        UUID repuesto = crearRepuesto(sku());
        UUID sucursal = crearSucursal();

        mockMvc.perform(post("/api/v1/inventory/stock/movements")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sparePartId\":\"" + repuesto + "\",\"branchId\":\"" + sucursal
                                + "\",\"movementType\":\"IN\",\"quantity\":10,\"unitCost\":25.00,"
                                + "\"idempotencyKey\":\"entrada-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/inventory/stock/balance/{partId}/branch/{branchId}", repuesto, sucursal)
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableStock").value(10))
                .andExpect(jsonPath("$.availableValue").value(250.0));
    }

    private String token() {
        return TestJwksServer.signToken(tenantId, List.of("ADMIN_TALLER"));
    }

    private UUID crearSucursal() {
        TenantContextHolder.setTenantId(tenantId);
        try {
            BranchResult resultado = branchUseCase.register(new RegisterBranchCommand(tenantId, "Sucursal", null));
            return resultado.id();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private UUID crearRepuesto(String sku) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            SparePartResult resultado = sparePartUseCase.register(
                    new RegisterSparePartCommand(tenantId, sku, "Filtro de aceite", 2));
            return resultado.id();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String sku() {
        return "SKU-" + Math.abs(UUID.randomUUID().hashCode() % 1000000);
    }
}
