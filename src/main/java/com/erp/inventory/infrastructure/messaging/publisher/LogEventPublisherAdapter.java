package com.erp.inventory.infrastructure.messaging.publisher;

import com.erp.inventory.domain.model.StockOutboxMessage;
import com.erp.inventory.domain.port.EventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Event publisher adapter for stock domain events.
 *
 * <p>Currently logs dispatched events while Kafka integration is pending.
 * The outbox pattern guarantees that the stock change and its outbox record commit atomically
 * in PostgreSQL, allowing this adapter to be swapped with a real message broker adapter
 * without modifying domain logic.
 */
@Component
public class LogEventPublisherAdapter implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LogEventPublisherAdapter.class);

    @Override
    public void publish(StockOutboxMessage message) {
        log.info("[OUTBOX-RELAY] Despachando evento de dominio '{}' para aggregateId='{}': {}",
                message.getEventType(), message.getAggregateId(), message.getPayload());
        // A Kafka or message broker producer will be integrated here when configured.
    }
}
