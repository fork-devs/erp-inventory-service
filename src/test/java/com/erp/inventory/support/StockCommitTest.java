package com.erp.inventory.support;

import com.erp.inventory.application.dto.command.RegisterBranchCommand;
import com.erp.inventory.application.dto.command.RegisterSparePartCommand;
import com.erp.inventory.application.dto.command.RegisterStockMovementCommand;
import com.erp.inventory.application.dto.command.ReserveStockCommand;
import com.erp.inventory.application.dto.result.BranchResult;
import com.erp.inventory.application.dto.result.SparePartResult;
import com.erp.inventory.application.usecase.BranchUseCase;
import com.erp.inventory.application.usecase.SparePartUseCase;
import com.erp.inventory.application.usecase.StockOperationUseCase;
import com.erp.inventory.domain.exception.InsufficientStockException;
import com.erp.inventory.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * La confirmacion consume la reserva, y solo la reserva.
 *
 * <p>Confirmar en silencio seria peor que fallar: el stock quedaria reservado
 * para siempre y la bitacora anotaria un consumo que no ha ocurrido.
 */
@SpringBootTest
class StockCommitTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private BranchUseCase branchUseCase;

    @Autowired
    private SparePartUseCase sparePartUseCase;

    @Autowired
    private StockOperationUseCase stockUseCase;

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

        entrada(10, "25.00");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Confirmar consume el stock reservado")
    void confirmarConsumeElStockReservado() {
        reservar(3, "reserva-" + UUID.randomUUID());

        stockUseCase.commit(new ReserveStockCommand(tenantId, repuesto, sucursal, 3,
                UUID.randomUUID(), "commit-" + UUID.randomUUID()));

        var saldo = stockUseCase.getBalance(repuesto, sucursal, tenantId);
        assertEquals(7, saldo.availableStock());
        assertEquals(0, saldo.totalReserved());
    }

    @Test
    @DisplayName("Confirmar una reserva ya confirmada se rechaza")
    void confirmarUnaReservaYaConfirmadaSeRechaza() {
        reservar(3, "reserva-" + UUID.randomUUID());
        stockUseCase.commit(new ReserveStockCommand(tenantId, repuesto, sucursal, 3,
                UUID.randomUUID(), "commit-" + UUID.randomUUID()));

        // Con OTRA clave: la reserva ya no tiene unidades reservadas.
        assertThrows(InsufficientStockException.class, () -> stockUseCase.commit(
                        new ReserveStockCommand(tenantId, repuesto, sucursal, 3,
                                UUID.randomUUID(), "commit-" + UUID.randomUUID())),
                "Confirmar dos veces la misma reserva tiene que rechazarse");

        var saldo = stockUseCase.getBalance(repuesto, sucursal, tenantId);
        assertEquals(7, saldo.availableStock(), "El stock no se consume dos veces");
    }

    @Test
    @DisplayName("Confirmar una reserva de otro taller no toca su stock")
    void confirmarUnaReservaDeOtroTallerNoTocaSuStock() {
        UUID otroTaller = UUID.randomUUID();
        TenantContextHolder.setTenantId(otroTaller);
        BranchResult otraSucursal = branchUseCase.register(new RegisterBranchCommand(otroTaller, "Otra", null));
        SparePartResult otroRepuesto = sparePartUseCase.register(
                new RegisterSparePartCommand(otroTaller, "SKU-" + Math.abs(UUID.randomUUID().hashCode() % 1000000),
                        "Otro", 2));
        stockUseCase.registerMovement(new RegisterStockMovementCommand(otroTaller, otroRepuesto.id(),
                otraSucursal.id(), "IN", 10, new BigDecimal("25.00"), null, "entrada-" + UUID.randomUUID()));
        stockUseCase.reserve(new ReserveStockCommand(otroTaller, otroRepuesto.id(), otraSucursal.id(), 3,
                UUID.randomUUID(), "reserva-" + UUID.randomUUID()));
        TenantContextHolder.clear();

        // El taller A intenta confirmar la reserva del taller B: no la ve.
        TenantContextHolder.setTenantId(tenantId);
        // El repuesto del otro taller no existe para este: la politica de
        // aislamiento no le muestra ni una fila.
        assertThrows(com.erp.inventory.domain.exception.DomainException.class,
                () -> stockUseCase.commit(
                        new ReserveStockCommand(tenantId, otroRepuesto.id(), otraSucursal.id(), 3,
                                UUID.randomUUID(), "commit-" + UUID.randomUUID())),
                "Confirmar la reserva de otro taller tiene que rechazarse");

        TenantContextHolder.setTenantId(otroTaller);
        var saldoDelOtro = stockUseCase.getBalance(otroRepuesto.id(), otraSucursal.id(), otroTaller);
        assertEquals(7, saldoDelOtro.availableStock(), "El stock del otro taller no se toca");
        assertEquals(3, saldoDelOtro.totalReserved(), "Su reserva sigue reservada");
    }

    private void reservar(int cantidad, String clave) {
        stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, cantidad,
                UUID.randomUUID(), clave));
    }

    private void entrada(int cantidad, String coste) {
        stockUseCase.registerMovement(new RegisterStockMovementCommand(tenantId, repuesto, sucursal,
                "IN", cantidad, new BigDecimal(coste), null, "entrada-" + UUID.randomUUID()));
    }
}
