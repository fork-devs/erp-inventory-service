package com.erp.inventory.application.usecase;

/**
 * Despacha los mensajes pendientes de la bandeja de salida.
 */
public interface ProcessOutboxMessagesUseCase {

    int processUnpublishedMessages(int batchSize);
}
