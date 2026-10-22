package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.StockOutboxMessage;

public interface EventPublisherPort {
    void publish(StockOutboxMessage message);
}
