package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.StockOutboxMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StockOutboxRepositoryPort {

    StockOutboxMessage save(StockOutboxMessage message);

    /**
     * Mensajes pendientes de publicar, cuya fecha de proximo intento ya ha
     * vencido. La reclamacion es exclusiva: dos llamadas concurrentes
     * obtienen conjuntos disjuntos.
     */
    List<StockOutboxMessage> findPending(Instant now, int limit);

    /**
     * Mensajes agotados, que ya no se reintentan solos pero se conservan.
     */
    List<StockOutboxMessage> findDeadLettered(UUID tenantId, int limit);

    /**
     * Devuelve un mensaje agotado a la cola de pendientes, con el contador de
     * intentos restablecido.
     */
    StockOutboxMessage reactivate(UUID id);
}
