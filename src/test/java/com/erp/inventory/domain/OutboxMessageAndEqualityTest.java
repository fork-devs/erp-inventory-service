package com.erp.inventory.domain;

import com.erp.inventory.domain.model.StockOutboxMessage;
import com.erp.inventory.domain.model.StockOutboxStatus;
import com.erp.inventory.domain.valueobject.BranchId;
import com.erp.inventory.domain.valueobject.IdempotencyKey;
import com.erp.inventory.domain.valueobject.Quantity;
import com.erp.inventory.domain.valueobject.Sku;
import com.erp.inventory.domain.valueobject.SparePartId;
import com.erp.inventory.domain.valueobject.StockLotId;
import com.erp.inventory.domain.valueobject.StockMovementId;
import com.erp.inventory.domain.valueobject.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El mensaje de la bandeja de salida y el contrato de igualdad de los value
 * objects.
 *
 * <p>La igualdad importa: el mensaje se guarda y se recupera de la base de
 * datos, y los identificadores se comparan en las operaciones de negocio.
 */
class OutboxMessageAndEqualityTest {

    @Test
    @DisplayName("Un mensaje nuevo esta pendiente y no tiene errores")
    void unMensajeNuevoEstaPendienteYNoTieneErrores() {
        StockOutboxMessage mensaje = StockOutboxMessage.of(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "StockReserved", "{}");

        assertEquals(StockOutboxStatus.PENDING, mensaje.getStatus());
        assertEquals(0, mensaje.getAttempts());
        assertNull(mensaje.getLastError());
        assertFalse(mensaje.isPublished());
        assertFalse(mensaje.isDeadLettered());
        assertNotNull(mensaje.getNextAttemptAt());
    }

    @Test
    @DisplayName("Un fallo cuenta el intento, guarda el error y programa el siguiente")
    void unFalloCuentaElIntentoGuardaElErrorYProgramaElSiguiente() {
        StockOutboxMessage mensaje = StockOutboxMessage.of(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "StockReserved", "{}");
        Instant antes = Instant.now();

        mensaje.recordFailure("el bus no responde", 7, Duration.ofHours(24));

        assertEquals(1, mensaje.getAttempts());
        assertEquals("el bus no responde", mensaje.getLastError());
        assertEquals(StockOutboxStatus.PENDING, mensaje.getStatus());
        assertTrue(mensaje.getNextAttemptAt().isAfter(antes));
    }

    @Test
    @DisplayName("Al agotar los intentos el mensaje pasa a cola de muertos y se conserva")
    void alAgotarLosIntentosElMensajePasaAColaDeMuertosYSeConserva() {
        StockOutboxMessage mensaje = StockOutboxMessage.of(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "StockReserved", "{}");

        mensaje.recordFailure("fallo", 2, Duration.ofHours(24));
        mensaje.recordFailure("fallo", 2, Duration.ofHours(24));

        assertTrue(mensaje.isDeadLettered());
        assertEquals(StockOutboxStatus.DEAD_LETTER, mensaje.getStatus());
        assertNull(mensaje.getNextAttemptAt(), "Un mensaje agotado no se programa solo");
        assertNotNull(mensaje.getPayload(), "El mensaje agotado se conserva");
    }

    @Test
    @DisplayName("Reactivar devuelve el mensaje a la cola de pendientes")
    void reactivarDevuelveElMensajeALaColaDePendientes() {
        StockOutboxMessage mensaje = StockOutboxMessage.of(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "StockReserved", "{}");
        mensaje.recordFailure("fallo", 1, Duration.ofHours(24));
        assertTrue(mensaje.isDeadLettered());

        mensaje.reactivate();

        assertEquals(0, mensaje.getAttempts());
        assertNull(mensaje.getLastError());
        assertEquals(StockOutboxStatus.PENDING, mensaje.getStatus());
        assertNotNull(mensaje.getNextAttemptAt());
    }

    @Test
    @DisplayName("Un mensaje publicado lo esta")
    void unMensajePublicadoLoEsta() {
        StockOutboxMessage mensaje = StockOutboxMessage.of(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "StockReserved", "{}");

        mensaje.markAsPublished();

        assertTrue(mensaje.isPublished());
        assertEquals(StockOutboxStatus.PUBLISHED, mensaje.getStatus());
        assertNull(mensaje.getLastError());
    }

    @Test
    @DisplayName("La espera entre reintentos es creciente y acotada")
    void laEsperaEntreReintentosEsCrecienteYAcotada() {
        StockOutboxMessage mensaje = StockOutboxMessage.of(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "StockReserved", "{}");

        // La espera crece con el numero de intentos: el primer reintento
        // espera 5 segundos y el segundo, 30.
        Duration primera = mensaje.nextDelay(Duration.ofHours(24));
        mensaje.recordFailure("fallo", 7, Duration.ofHours(24));
        mensaje.recordFailure("fallo", 7, Duration.ofHours(24));
        Duration segunda = mensaje.nextDelay(Duration.ofHours(24));

        assertTrue(segunda.compareTo(primera) > 0, "La espera crece: " + primera + " -> " + segunda);
        assertTrue(primera.compareTo(Duration.ofHours(24)) <= 0, "La espera no supera el maximo");
    }

    @Test
    @DisplayName("Los value objects se comparan por su valor")
    void losValueObjectsSeComparanPorSuValor() {
        UUID uuid = UUID.randomUUID();

        assertEquals(new BranchId(uuid), new BranchId(uuid));
        assertEquals(new BranchId(uuid).hashCode(), new BranchId(uuid).hashCode());
        assertNotEquals(new BranchId(uuid), new BranchId(UUID.randomUUID()));
        assertNotEquals(new BranchId(uuid), "no es un identificador");

        assertEquals(new SparePartId(uuid), new SparePartId(uuid));
        assertEquals(new StockMovementId(uuid), new StockMovementId(uuid));
        assertEquals(new StockLotId(uuid), new StockLotId(uuid));
        assertEquals(new TenantId(uuid), new TenantId(uuid));
        assertEquals(new Quantity(3), new Quantity(3));
        assertEquals(new Sku("ABC-1"), new Sku("ABC-1"));
        assertEquals(new IdempotencyKey("k-1"), new IdempotencyKey("k-1"));
    }

    @Test
    @DisplayName("Los value objects se muestran con su valor")
    void losValueObjectsSeMuestranConSuValor() {
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid.toString(), new BranchId(uuid).toString());
        assertEquals(uuid.toString(), new SparePartId(uuid).toString());
        assertEquals(uuid.toString(), new StockMovementId(uuid).toString());
        assertEquals(uuid.toString(), new StockLotId(uuid).toString());
        assertEquals(uuid.toString(), new TenantId(uuid).toString());
        assertEquals("3", new Quantity(3).toString());
        assertEquals("ABC-1", new Sku("ABC-1").toString());
        assertEquals("k-1", new IdempotencyKey("k-1").toString());
    }
}
