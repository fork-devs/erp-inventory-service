package com.erp.inventory.application.usecase;

/**
 * Dispatches pending messages from the outbox table to the event bus.
 */
public interface ProcessOutboxMessagesUseCase {

    int processUnpublishedMessages(int batchSize);
}
