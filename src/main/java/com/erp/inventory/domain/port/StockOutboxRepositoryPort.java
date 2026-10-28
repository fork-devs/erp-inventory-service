package com.erp.inventory.domain.port;

import com.erp.inventory.domain.model.StockOutboxMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface StockOutboxRepositoryPort {

    StockOutboxMessage save(StockOutboxMessage message);

    /**
     * Retrieves pending messages due for publication. Claiming is exclusive
     * so that concurrent relay instances receive disjoint message sets.
     */
    List<StockOutboxMessage> findPending(Instant now, int limit);

    /**
     * Retrieves dead-lettered messages that are no longer automatically retried.
     */
    List<StockOutboxMessage> findDeadLettered(UUID tenantId, int limit);

    /**
     * Reactivates a dead-lettered message, returning it to the pending queue with reset attempts.
     */
    StockOutboxMessage reactivate(UUID id);
}
