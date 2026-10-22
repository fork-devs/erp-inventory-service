package com.erp.inventory.support;

import com.erp.inventory.application.dto.command.RegisterBranchCommand;
import com.erp.inventory.application.dto.command.RegisterSparePartCommand;
import com.erp.inventory.application.dto.command.RegisterStockMovementCommand;
import com.erp.inventory.application.dto.command.ReserveStockCommand;
import com.erp.inventory.application.dto.result.BranchResult;
import com.erp.inventory.application.dto.result.SparePartResult;
import com.erp.inventory.application.usecase.BranchUseCase;
import com.erp.inventory.application.usecase.ProcessOutboxMessagesUseCase;
import com.erp.inventory.application.usecase.SparePartUseCase;
import com.erp.inventory.application.usecase.StockOperationUseCase;
import com.erp.inventory.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El evento de stock viaja con el cambio que lo origina.
 *
 * <p>Publicar el evento DENTRO de la transaccion que confirma el cambio de
 * stock es justo lo que la bandeja evita: el bus podria recibir un evento que
 * la base de datos no llego a confirmar, y un fallo del bus perderia el
 * evento sin revertir el stock.
 */
@SpringBootTest
class StockOutboxTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private BranchUseCase branchUseCase;

    @Autowired
    private SparePartUseCase sparePartUseCase;

    @Autowired
    private StockOperationUseCase stockUseCase;

    @Autowired
    private ProcessOutboxMessagesUseCase processOutboxMessagesUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID tenantId;
    private UUID repuesto;
    private UUID sucursal;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        BranchResult sucursalCreada = branchUseCase.register(new RegisterBranchCommand(tenantId, "Sucursal central", null));
        sucursal = sucursalCreada.id();

        SparePartResult repuestoCreado = sparePartUseCase.register(
                new RegisterSparePartCommand(tenantId, "SKU-" + Math.abs(UUID.randomUUID().hashCode() % 1000000),
                        "Filtro de aceite", 2));
        repuesto = repuestoCreado.id();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("La reserva deja el evento en la bandeja, en la misma transaccion")
    void laReservaDejaElEventoEnLaBandeja() {
        entrada(10, "25.00");

        stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 3,
                UUID.randomUUID(), "reserva-" + UUID.randomUUID()));

        assertEquals(1, mensajesEnBandeja(), "La reserva deja su evento en la bandeja");
        assertTrue(tipoEnBandeja().contains("StockReserved"),
                "El evento de la bandeja es el de la reserva: " + tipoEnBandeja());
    }

    @Test
    @DisplayName("Una entrada no deja evento: no es un cambio de stock relevante")
    void unaEntradaNoDejaEvento() {
        entrada(10, "25.00");

        assertEquals(0, mensajesEnBandeja(),
                "Una entrada de stock no es un cambio de stock relevante para el taller");
    }

    @Test
    @DisplayName("El relay publica los mensajes pendientes y los marca como publicados")
    void elRelayPublicaLosMensajesPendientes() {
        entrada(10, "25.00");
        stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 3,
                UUID.randomUUID(), "reserva-" + UUID.randomUUID()));

        processOutboxMessagesUseCase.processUnpublishedMessages(50);

        assertEquals(0, mensajesPendientes(), "El mensaje publicado sale de la cola de pendientes");
        assertEquals(1, mensajesPublicados(), "El mensaje queda como publicado");
    }

    @Test
    @DisplayName("El evento lleva el identificador del taller y el del movimiento")
    void elEventoLlevaElIdentificadorDelTallerYElDelMovimiento() {
        entrada(10, "25.00");
        var resultado = stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 3,
                UUID.randomUUID(), "reserva-" + UUID.randomUUID()));

        String payload = jdbcTemplate.queryForObject(
                "SELECT payload FROM stock_outbox WHERE tenant_id = ? ORDER BY created_at DESC LIMIT 1",
                String.class, tenantId);

        assertTrue(payload.contains(tenantId.toString()), "El evento lleva el taller: " + payload);
        assertTrue(payload.contains(resultado.id().toString()), "El evento lleva el movimiento: " + payload);
    }

    private void entrada(int cantidad, String coste) {
        stockUseCase.registerMovement(new RegisterStockMovementCommand(tenantId, repuesto, sucursal,
                "IN", cantidad, new BigDecimal(coste), null, "entrada-" + UUID.randomUUID()));
    }

    private int mensajesEnBandeja() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_outbox WHERE tenant_id = ?", Integer.class, tenantId);
    }

    private int mensajesPublicados() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_outbox WHERE status = 'PUBLISHED' AND tenant_id = ?",
                Integer.class, tenantId);
    }

    private int mensajesPendientes() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM stock_outbox WHERE status = 'PENDING' AND tenant_id = ?",
                Integer.class, tenantId);
    }

    private String tipoEnBandeja() {
        return jdbcTemplate.queryForObject(
                "SELECT event_type FROM stock_outbox WHERE tenant_id = ? ORDER BY created_at DESC LIMIT 1",
                String.class, tenantId);
    }
}
