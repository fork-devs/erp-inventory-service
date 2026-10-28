package com.erp.inventory.domain.model;

/**
 * Status of an outbox message.
 *
 * <p>State transitions are explicit rather than boolean: a boolean flag cannot
 * distinguish between newly queued messages, repeatedly failing messages,
 * or permanently dead-lettered entries.
 */
public enum StockOutboxStatus {

    /** Pending publication with next attempt scheduled. */
    PENDING,

    /** Successfully published to the event bus. */
    PUBLISHED,

    /** Exhausted all retry attempts; preserved without automatic retry. */
    DEAD_LETTER
}
