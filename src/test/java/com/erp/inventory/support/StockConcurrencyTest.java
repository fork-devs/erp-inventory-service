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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Multi-threaded concurrency tests verifying idempotency and race condition prevention.
 *
 * <p>Executes parallel threads to verify that concurrent requests with identical idempotency keys
 * register only one movement, and simultaneous reservations cannot over-reserve available stock.
 */
@SpringBootTest
class StockConcurrencyTest extends AbstractPostgresIntegrationTest {

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
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Dos peticiones concurrentes con la misma clave registran un solo movimiento")
    void dosPeticionesConcurrentesConLaMismaClaveRegistranUnSoloMovimiento() throws Exception {
        entrada(10, "25.00");
        String clave = "reserva-" + UUID.randomUUID();

        AtomicInteger exitos = new AtomicInteger();
        List<String> ids = new CopyOnWriteArrayList<>();
        CountDownLatch arranque = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    TenantContextHolder.setTenantId(tenantId);
                    arranque.await(5, TimeUnit.SECONDS);
                    var resultado = stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 3,
                            UUID.randomUUID(), clave));
                    exitos.incrementAndGet();
                    ids.add(resultado.id().toString());
                } catch (Exception e) {
                    // One of the concurrent requests may fail; both must not register distinct movements.
                } finally {
                    TenantContextHolder.clear();
                }
            });
        }

        arranque.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        assertEquals(2, exitos.get(),
                "Las dos peticiones con la misma clave deben devolver el mismo resultado");
        assertEquals(1, ids.stream().distinct().count(),
                "Las dos peticiones devuelven el MISMO movimiento");
        assertEquals(3, stockUseCase.getBalance(repuesto, sucursal, tenantId).totalReserved(),
                "La reserva se cuenta UNA vez, no dos");
    }

    @Test
    @DisplayName("Dos reservas simultaneas no pueden sobre-reservar")
    void dosReservasSimultaneasNoPuedenSobreReservar() throws Exception {
        entrada(5, "10.00");

        AtomicInteger aceptadas = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();
        CountDownLatch arranque = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    TenantContextHolder.setTenantId(tenantId);
                    arranque.await(5, TimeUnit.SECONDS);
                    stockUseCase.reserve(new ReserveStockCommand(tenantId, repuesto, sucursal, 4,
                            UUID.randomUUID(), "reserva-" + UUID.randomUUID()));
                    aceptadas.incrementAndGet();
                } catch (InsufficientStockException e) {
                    rechazadas.incrementAndGet();
                } catch (Exception e) {
                    // Any other failure is treated as a rejection.
                    rechazadas.incrementAndGet();
                } finally {
                    TenantContextHolder.clear();
                }
            });
        }

        arranque.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        var saldo = stockUseCase.getBalance(repuesto, sucursal, tenantId);
        assertTrue(saldo.availableStock() >= 0,
                "El disponible no puede quedar en negativo: " + saldo.availableStock());
        assertTrue(saldo.totalReserved() <= 5,
                "No se puede reservar mas de lo que hay: " + saldo.totalReserved());
        assertEquals(2, aceptadas.get() + rechazadas.get(),
                "Las dos peticiones se atienden: una entra y la otra se rechaza");
        assertEquals(4, saldo.totalReserved(),
                "Solo UNA de las dos reservas cabe en el stock: la otra se rechaza");
        assertEquals(1, saldo.availableStock(),
                "Queda el stock que no se pudo reservar dos veces");
    }

    private void entrada(int cantidad, String coste) {
        stockUseCase.registerMovement(new RegisterStockMovementCommand(tenantId, repuesto, sucursal,
                "IN", cantidad, new BigDecimal(coste), null, "entrada-" + UUID.randomUUID()));
    }
}
