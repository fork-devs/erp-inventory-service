package com.erp.inventory.domain;

import com.erp.inventory.domain.model.Branch;
import com.erp.inventory.domain.model.MovementType;
import com.erp.inventory.domain.model.SparePart;
import com.erp.inventory.domain.model.StockMovement;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.IdempotencyKey;
import com.erp.inventory.domain.valueobject.Quantity;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.StockMovementId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Domain tests verifying inventory value objects and entity invariants.
 *
 * <p>Ensures user input is normalized and validated before reaching persistence layers.
 */
class InventoryDomainTest {

    private static final TenantId TALLER = new TenantId(UUID.randomUUID());

    @Test
    @DisplayName("El SKU se normaliza a mayusculas")
    void elSkuSeNormalizaAMayusculas() {
        assertEquals("ABC-123", Sku.of("  abc-123 ").getValue());
        assertEquals(Sku.of("abc-123"), Sku.of("ABC-123"));
        assertEquals(Sku.of("abc-123").hashCode(), Sku.of("ABC-123").hashCode());
        assertNotEquals(Sku.of("abc-123"), Sku.of("abc-124"));
    }

    @Test
    @DisplayName("El SKU no puede estar vacio")
    void elSkuNoPuedeEstarVacio() {
        assertThrows(IllegalArgumentException.class, () -> Sku.of(""));
        assertThrows(IllegalArgumentException.class, () -> Sku.of("   "));
        assertThrows(IllegalArgumentException.class, () -> Sku.of(null));
    }

    @Test
    @DisplayName("La cantidad ha de ser mayor que cero")
    void laCantidadHaDeSerMayorQueCero() {
        assertEquals(5, Quantity.of(5).getValue());
        assertThrows(IllegalArgumentException.class, () -> Quantity.of(0));
        assertThrows(IllegalArgumentException.class, () -> Quantity.of(-1));
    }

    @Test
    @DisplayName("La clave de idempotencia no puede estar vacia")
    void laClaveDeIdempotenciaNoPuedeEstarVacia() {
        assertEquals("clave-1", IdempotencyKey.of("  clave-1 ").getValue());
        assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.of(" "));
        assertThrows(IllegalArgumentException.class, () -> IdempotencyKey.of(null));
    }

    @Test
    @DisplayName("Los identificadores no pueden ser nulos y se reconstruyen desde su texto")
    void losIdentificadoresNoPuedenSerNulosYSeReconstruyenDesdeSuTexto() {
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid, new BranchId(uuid).getValue());
        assertEquals(uuid, new SparePartId(uuid).getValue());
        assertEquals(uuid, new StockMovementId(uuid).getValue());
        assertThrows(IllegalArgumentException.class, () -> new BranchId(null));
        assertThrows(IllegalArgumentException.class, () -> new SparePartId(null));
        assertThrows(IllegalArgumentException.class, () -> new StockMovementId(null));
        assertThrows(IllegalArgumentException.class, () -> new TenantId(null));
    }

    @Test
    @DisplayName("La sucursal exige un nombre y recorta los espacios")
    void laSucursalExigeUnNombreYRecortaLosEspacios() {
        Branch sucursal = Branch.create(TALLER, "  Sucursal central  ", "Av. Siempre Viva 742");

        assertEquals("Sucursal central", sucursal.getName());
        assertEquals(TALLER, sucursal.getTenantId());
        assertThrows(IllegalArgumentException.class, () -> Branch.create(TALLER, "  ", null));
        assertThrows(IllegalArgumentException.class, () -> Branch.create(null, "Sucursal", null));
    }

    @Test
    @DisplayName("El repuesto exige descripcion y su umbral no puede ser negativo")
    void elRepuestoExigeDescripcionYSuUmbralNoPuedeSerNegativo() {
        SparePart repuesto = SparePart.create(TALLER, Sku.of("ABC-1"), "Filtro de aceite", 3);

        assertEquals(3, repuesto.getMinStock());
        assertThrows(IllegalArgumentException.class,
                () -> SparePart.create(TALLER, Sku.of("ABC-1"), "  ", 1));
        assertThrows(IllegalArgumentException.class,
                () -> SparePart.create(TALLER, Sku.of("ABC-1"), "Filtro", -1));
        assertEquals("Filtro de aceite", repuesto.getDescription());
        assertEquals(Sku.of("ABC-1"), repuesto.getSku());
    }

    @Test
    @DisplayName("El movimiento de stock exige sus campos y conoce su tipo")
    void elMovimientoDeStockExigeSusCamposYConoceSuTipo() {
        StockMovement movimiento = StockMovement.create(TALLER, new SparePartId(UUID.randomUUID()),
                new BranchId(UUID.randomUUID()), MovementType.IN, Quantity.of(3),
                UUID.randomUUID(), IdempotencyKey.of("clave-1"));

        assertEquals(MovementType.IN, movimiento.getMovementType());
        assertEquals(3, movimiento.getQuantity().getValue());
        assertEquals("clave-1", movimiento.getIdempotencyKey().getValue());

        assertThrows(IllegalArgumentException.class, () -> StockMovement.create(null,
                new SparePartId(UUID.randomUUID()), new BranchId(UUID.randomUUID()),
                MovementType.IN, Quantity.of(1), null, null));
    }

    @Test
    @DisplayName("El conjunto de tipos de movimiento incluye la consumicion de la reserva")
    void elConjuntoDeTiposDeMovimientoIncluyeLaConsumicionDeLaReserva() {
        assertEquals(5, MovementType.values().length);
        assertEquals(MovementType.COMMIT, MovementType.valueOf("COMMIT"));
    }
}
