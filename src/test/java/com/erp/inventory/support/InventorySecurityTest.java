package com.erp.inventory.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying authentication and authorization perimeter enforcement.
 *
 * <p>Endpoints require a valid JWT token, with tenant context derived exclusively
 * from token claims rather than unauthenticated request headers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InventorySecurityTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Una peticion sin token se rechaza y no registra ningun movimiento")
    void unaPeticionSinTokenSeRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/inventory/stock/movements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sparePartId\":\"" + UUID.randomUUID() + "\",\"branchId\":\""
                                + UUID.randomUUID() + "\",\"movementType\":\"IN\",\"quantity\":5}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un rol sin permiso de escritura recibe 403")
    void unRolSinPermisoDeEscrituraRecibe403() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("MECANICO"));

        mockMvc.perform(post("/api/v1/inventory/stock/movements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sparePartId\":\"" + UUID.randomUUID() + "\",\"branchId\":\""
                                + UUID.randomUUID() + "\",\"movementType\":\"IN\",\"quantity\":5}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un rol de solo lectura puede consultar")
    void unRolDeSoloLecturaPuedeConsultar() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("MECANICO"));

        mockMvc.perform(get("/api/v1/inventory/spare-parts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un rol con permiso de escritura puede registrar")
    void unRolConPermisoDeEscrituraPuedeRegistrar() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));

        mockMvc.perform(post("/api/v1/inventory/branches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sucursal de prueba\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Una cabecera de taller que no coincide con el token se rechaza")
    void unaCabeceraDeTallerDistintaDelTokenSeRechaza() throws Exception {
        UUID tallerDelToken = UUID.randomUUID();
        UUID otroTaller = UUID.randomUUID();
        String token = TestJwksServer.signToken(tallerDelToken, List.of("ADMIN_TALLER"));

        mockMvc.perform(get("/api/v1/inventory/spare-parts")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-ID", otroTaller.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Una cabecera de taller que coincide con el token se acepta")
    void unaCabeceraDeTallerCoincidenteSeAcepta() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));

        mockMvc.perform(get("/api/v1/inventory/spare-parts")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-ID", taller.toString()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un token con firma invalida se rechaza")
    void unTokenConFirmaInvalidaSeRechaza() throws Exception {
        String token = TestJwksServer.signToken(UUID.randomUUID(), List.of("ADMIN_TALLER"));
        String adulterado = token.substring(0, token.length() - 4) + "abcd";

        mockMvc.perform(get("/api/v1/inventory/spare-parts").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rol TECHNICIAN puede consultar productos pero no crearlos")
    void rolTechnicianPuedeConsultarProductosPeroNoCrearlos() throws Exception {
        UUID taller = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("TECHNICIAN"));

        mockMvc.perform(get("/api/v1/inventory/products")
                        .header("Authorization", "Bearer " + token)
                        .param("branchId", branchId.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/inventory/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"branchId\":\"" + branchId + "\",\"category\":\"Filters\",\"name\":\"Filter\",\"sku\":\"F-1\",\"salePrice\":10,\"minimumStock\":1}"))
                .andExpect(status().isForbidden());
    }
}
