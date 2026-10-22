package com.erp.inventory.infrastructure.messaging.publisher;

import com.erp.inventory.domain.model.StockOutboxMessage;
import com.erp.inventory.domain.port.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Publicador de los eventos de stock.
 *
 * <p>Hoy registra el evento: <strong>Kafka todavia no existe</strong> en la
 * plataforma. Lo que importa es que el cambio de stock y su evento se
 * confirman juntos en la base de datos, y que el relay los publique despues:
 * cuando el bus exista, esta clase cambia y la bandeja no.
 */
@Component
public class LogEventPublisherAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LogEventPublisherAdapter.class);

    @Override
    public void publish(StockOutboxMessage message) {
        log.info("[OUTBOX-RELAY] Despachando evento de dominio '{}' para aggregateId='{}': {}",
                message.getEventType(), message.getAggregateId(), message.getPayload());
        // Aqui se conectara el productor de Kafka cuando el broker exista.
    }
}
