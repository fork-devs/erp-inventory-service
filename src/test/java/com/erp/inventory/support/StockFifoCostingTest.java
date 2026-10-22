package com.erp.inventory.support;

import com.erp.inventory.application.dto.command.RegisterBranchCommand;
import com.erp.inventory.application.dto.command.RegisterSparePartCommand;
import com.erp.inventory.application.dto.command.RegisterStockMovementCommand;
import com.erp.inventory.application.dto.command.ReserveStockCommand;
import com.erp.inventory.application.dto.result.BranchResult;
import com.erp.inventory.application.dto.result.SparePartResult;
import com.erp.inventory.application.dto.result.StockBalanceResult;
import com.erp.inventory.application.usecase.BranchUseCase;
import com.erp.inventory.application.usecase.SparePartUseCase;
import com.erp.inventory.application.usecase.StockOperationUseCase;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El saldo se calcula por lotes, en orden FIFO, y con su valor.
 *
 * <p>Sin lotes, el saldo dice cuantas unidades hay, pero no cuanto valen: el
 * coste de lo consumido se pierde en el momento en que se consume.
 */
@SpringBootTest
class StockFifoCostingTest extends AbstractPostgresIntegrationTest {

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
                new RegisterSparePartCommand(tenantId, sku(), "Filtro de aceite", 2));
        repuesto = repuestoCreado.id();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Una entrada abre un lote con su coste y valoriza el stock")
    void unaEntradaAbreUnLoteConSuCoste() {
        entrada(10, "25.00");

        StockBalanceResult saldo = saldo();

        assertEquals(10, saldo.availableStock());
        assertEquals(0, saldo.totalReserved());
        assertEquals(0, new BigDecimal("250.00").compareTo(saldo.availableValue()),
                "El valor del stock sale del coste real del lote");
    }

    @Test
    @DisplayName("El consumo toma primero el lote mas antiguo")
    void elConsumoTomaPrimeroElLoteMasAntiguo() {
        entrada(5, "10.00");
        entrada(5, "20.00");

        // Se reserva 7 unidades: el saldo debe reflejar que el lote mas
        // antiguo (a 10) se consumio primero.
        stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 7,
                UUID.randomUUID(), "reserva-" + UUID.randomUUID()));

        StockBalanceResult saldo = saldo();

        assertEquals(3, saldo.availableStock());
        // Quedan 3 unidades del lote viejo (a 10) y ninguna del nuevo: el
        // valor lo confirma.
        // FIFO consume primero el lote MAS ANTIGUO: se van las 5 unidades a
        // 10 y 2 de las 5 a 20. Quedan 3 unidades a 20, y reservadas valen
        // 5x10 + 2x20.
        assertEquals(0, new BigDecimal("60.00").compareTo(saldo.availableValue()),
                "El consumo FIFO deja las unidades mas RECIENTES, con su coste mas alto");
        assertEquals(0, new BigDecimal("90.00").compareTo(saldo.reservedValue()),
                "Lo reservado vale lo que valia cada lote consumido");
    }

    @Test
    @DisplayName("El valor reservado y el disponible se tellan por separado")
    void elValorReservadoYElDisponibleSeTellanPorSeparado() {
        entrada(10, "25.00");

        stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 4,
                UUID.randomUUID(), "reserva-" + UUID.randomUUID()));

        StockBalanceResult saldo = saldo();

        assertEquals(6, saldo.availableStock());
        assertEquals(4, saldo.totalReserved());
        assertEquals(0, new BigDecimal("150.00").compareTo(saldo.availableValue()));
        assertEquals(0, new BigDecimal("100.00").compareTo(saldo.reservedValue()));
    }

    @Test
    @DisplayName("La confirmacion consume el stock reservado")
    void laConfirmacionConsumeElStockReservado() {
        entrada(10, "25.00");
        String clave = "reserva-" + UUID.randomUUID();

        stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 4,
                UUID.randomUUID(), clave));
        stockUseCase.commit(new ReserveStockCommand(tenantId, repuesto, sucursal, 4,
                UUID.randomUUID(), "commit-" + UUID.randomUUID()));

        StockBalanceResult saldo = saldo();

        assertEquals(6, saldo.availableStock(), "La confirmacion consume el stock reservado");
        assertEquals(0, saldo.totalReserved());
        assertEquals(0, new BigDecimal("150.00").compareTo(saldo.availableValue()));
    }

    @Test
    @DisplayName("La liberacion devuelve el stock reservado al disponible")
    void laLiberacionDevuelveElStockReservado() {
        entrada(10, "25.00");

        stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 4,
                UUID.randomUUID(), "reserva-" + UUID.randomUUID()));
        stockUseCase.release(new com.erp.inventory.application.dto.command.ReleaseStockCommand(
                tenantId, repuesto, sucursal, 4, UUID.randomUUID(), "release-" + UUID.randomUUID()));

        StockBalanceResult saldo = saldo();

        assertEquals(10, saldo.availableStock());
        assertEquals(0, saldo.totalReserved());
    }

    @Test
    @DisplayName("Una reserva por encima del disponible se rechaza")
    void unaReservaPorEncimaDelDisponibleSeRechaza() {
        entrada(3, "10.00");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.erp.inventory.domain.exception.InsufficientStockException.class,
                () -> stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 5,
                        UUID.randomUUID(), "reserva-" + UUID.randomUUID())));

        assertEquals(3, saldo().availableStock(), "La reserva rechazada no altera el stock");
    }

    private void entrada(int cantidad, String coste) {
        stockUseCase.registerMovement(new RegisterStockMovementCommand(tenantId, repuesto, sucursal,
                "IN", cantidad, new BigDecimal(coste), null, "entrada-" + UUID.randomUUID()));
    }

    private StockBalanceResult saldo() {
        return stockUseCase.getBalance(repuesto, sucursal, tenantId);
    }

    private String sku() {
        return "SKU-" + Math.abs(UUID.randomUUID().hashCode() % 1000000);
    }
}
