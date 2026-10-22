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
import com.erp.inventory.domain.port.UnitOfWork;
import com.erp.inventory.domain.valueobject.TenantId;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aislamiento entre talleres.
 *
 * <p>El atacante es el ADMIN_TALLER del taller A y la victima es el taller B.
 * Los vectores: leer, escribir, reservar y confirmar.
 *
 * <p>El aislamiento no depende de que cada consulta recuerde filtrar por
 * taller: lo impone la politica de Row-Level Security, y el contexto lo fija el
 * principal autenticado.
 */
@SpringBootTest
class CrossTenantAccessTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private BranchUseCase branchUseCase;

    @Autowired
    private SparePartUseCase sparePartUseCase;

    @Autowired
    private StockOperationUseCase stockUseCase;

    @Autowired
    private UnitOfWork unitOfWork;

    private UUID tallerA;
    private UUID tallerB;

    @BeforeEach
    void setUp() {
        tallerA = UUID.randomUUID();
        tallerB = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("El taller A no lee los repuestos del taller B")
    void elTallerANoLeeLosRepuestosDelTallerB() {
        UUID repuesto = crearRepuesto(tallerB);

        TenantContextHolder.setTenantId(tallerA);
        assertTrue(repuestosDe(tallerA).isEmpty(),
                "La politica de aislamiento no puede devolver un repuesto de otro taller");
        assertTrue(repuestosDe(tallerA).stream().noneMatch(r -> r.equals(repuesto)));
    }

    @Test
    @DisplayName("El taller A no ve el saldo de un repuesto del taller B")
    void elTallerANoVeElSaldoDeUnRepuestoDelTallerB() {
        UUID repuesto = crearRepuesto(tallerB);
        UUID sucursal = crearSucursal(tallerB);

        // El taller B tiene stock de ese repuesto.
        TenantContextHolder.setTenantId(tallerB);
        stockUseCase.registerMovement(new RegisterStockMovementCommand(tallerB, repuesto, sucursal,
                "IN", 10, new BigDecimal("25.00"), null, "entrada-" + UUID.randomUUID()));
        TenantContextHolder.clear();

        // El taller A no ve ni una unidad: la politica filtra las filas.
        TenantContextHolder.setTenantId(tallerA);
        var saldo = stockUseCase.getBalance(repuesto, sucursal, tallerA);

        assertEquals(0, saldo.availableStock(),
                "El saldo de un repuesto de otro taller tiene que estar vacio");
        assertEquals(0, saldo.totalIn(),
                "El taller A no puede ver las entradas del taller B");
    }

    @Test
    @DisplayName("El taller A no puede reservar stock de un repuesto del taller B")
    void elTallerANoPuedeReservarStockDeUnRepuestoDelTallerB() {
        UUID repuesto = crearRepuesto(tallerB);
        UUID sucursal = crearSucursal(tallerB);

        TenantContextHolder.setTenantId(tallerA);
        assertThrows(RuntimeException.class, () -> stockUseCase.reserve(
                        new ReserveStockCommand(tallerA, repuesto, sucursal, 1,
                                UUID.randomUUID(), "reserva-" + UUID.randomUUID())),
                "Reservar stock de un repuesto de otro taller tiene que rechazarse");
    }

    @Test
    @DisplayName("El listado del taller A no incluye los repuestos del taller B")
    void elListadoDelTallerANoIncluyeLosRepuestosDelTallerB() {
        crearRepuesto(tallerA);
        crearRepuesto(tallerB);

        TenantContextHolder.setTenantId(tallerA);

        assertEquals(1, repuestosDe(tallerA).size(),
                "El listado debe contener solo los repuestos del taller del principal");
    }

    private java.util.List<SparePartResult> repuestosDe(UUID tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            return sparePartUseCase.listAll(tenantId);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private UUID crearRepuesto(UUID tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            SparePartResult resultado = sparePartUseCase.register(
                    new RegisterSparePartCommand(tenantId,
                            "SKU-" + Math.abs(UUID.randomUUID().hashCode() % 1000000),
                            "Filtro de aceite", 2));
            return resultado.id();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private UUID crearSucursal(UUID tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            BranchResult resultado = branchUseCase.register(new RegisterBranchCommand(tenantId, "Sucursal", null));
            return resultado.id();
        } finally {
            TenantContextHolder.clear();
        }
    }
}
