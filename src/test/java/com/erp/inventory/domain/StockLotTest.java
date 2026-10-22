package com.erp.inventory.domain;

import com.erp.inventory.domain.model.StockLot;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * El lote es la unidad de coste: se abre con una entrada, se consume por
 * orden de llegada y su valor es el de las unidades que quedan.
 */
class StockLotTest {

    private static final TenantId TALLER = new TenantId(UUID.randomUUID());
    private static final SparePartId REPUESTO = new SparePartId(UUID.randomUUID());
    private static final BranchId SUCURSAL = new BranchId(UUID.randomUUID());

    @Test
    @DisplayName("Un lote recien abierto tiene todas sus unidades libres")
    void unLoteRecienAbiertoTieneTodasSusUnidadesLibres() {
        StockLot lote = StockLot.open(TALLER, REPUESTO, SUCURSAL, 10, new BigDecimal("25.00"), Instant.now());

        assertEquals(10, lote.getQuantity());
        assertEquals(0, lote.getReservedQuantity());
        assertEquals(10, lote.getAvailableQuantity());
        assertEquals(new BigDecimal("250.00"), lote.getAvailableValue());
    }

    @Test
    @DisplayName("Reservar unidades las saca del disponible y las cuenta como reservadas")
    void reservarUnidadesLasSacaDelDisponible() {
        StockLot lote = StockLot.open(TALLER, REPUESTO, SUCURSAL, 10, new BigDecimal("25.00"), Instant.now());

        lote.reserve(4);

        assertEquals(6, lote.getAvailableQuantity());
        assertEquals(4, lote.getReservedQuantity());
        assertEquals(new BigDecimal("100.00"), lote.getReservedValue());
        assertEquals(new BigDecimal("150.00"), lote.getAvailableValue());
    }

    @Test
    @DisplayName("Liberar devuelve las unidades reservadas al disponible")
    void liberarDevuelveLasUnidadesReservadas() {
        StockLot lote = StockLot.open(TALLER, REPUESTO, SUCURSAL, 10, new BigDecimal("25.00"), Instant.now());
        lote.reserve(4);

        lote.release(4);

        assertEquals(10, lote.getAvailableQuantity());
        assertEquals(0, lote.getReservedQuantity());
    }

    @Test
    @DisplayName("Consumir reduce el lote y su valor")
    void consumirReduceElLoteYSuValor() {
        StockLot lote = StockLot.open(TALLER, REPUESTO, SUCURSAL, 10, new BigDecimal("25.00"), Instant.now());

        lote.consume(3);

        assertEquals(7, lote.getQuantity());
        assertEquals(7, lote.getAvailableQuantity());
        assertEquals(new BigDecimal("175.00"), lote.getAvailableValue());
    }

    @Test
    @DisplayName("Consumir lo reservado lo convierte en consumo real")
    void consumirLoReservadoLoConvierteEnConsumoReal() {
        StockLot lote = StockLot.open(TALLER, REPUESTO, SUCURSAL, 10, new BigDecimal("25.00"), Instant.now());
        lote.reserve(4);

        lote.consumeReserved(4);

        assertEquals(6, lote.getQuantity());
        assertEquals(0, lote.getReservedQuantity());
        assertEquals(6, lote.getAvailableQuantity());
    }

    @Test
    @DisplayName("No se puede reservar mas de lo que hay, ni consumir unidades que no existen")
    void noSePuedeReservarMasDeLoQueHay() {
        StockLot lote = StockLot.open(TALLER, REPUESTO, SUCURSAL, 3, new BigDecimal("10.00"), Instant.now());

        assertThrows(IllegalStateException.class, () -> lote.reserve(4));
        assertThrows(IllegalStateException.class, () -> lote.consume(4));
        assertThrows(IllegalStateException.class, () -> lote.release(1));
    }

    @Test
    @DisplayName("Un lote con cantidad no positiva se rechaza")
    void unLoteConCantidadNoPositivaSeRechaza() {
        assertThrows(IllegalArgumentException.class,
                () -> StockLot.open(TALLER, REPUESTO, SUCURSAL, 0, new BigDecimal("10.00"), Instant.now()));
    }
}
