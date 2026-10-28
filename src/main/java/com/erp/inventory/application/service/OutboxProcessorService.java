package com.erp.inventory.application.service;

import com.erp.inventory.application.usecase.ProcessOutboxMessagesUseCase;
import com.erp.inventory.domain.model.StockOutboxMessage;
import com.erp.inventory.domain.port.EventPublisherPort;
import com.erp.inventory.domain.port.StockOutboxRepositoryPort;
import com.erp.inventory.domain.port.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Dispatches pending outbox messages to the event bus.
 *
 * <p>Key architectural properties:
 *
 * <ul>
 *   <li>A failing message <strong>does not block</strong> the rest of the queue.
 *       Previously, a single poison pill message permanently halted the entire batch
 *       because the queue was ordered by creation timestamp, keeping the failed one at the front.</li>
 *   <li>Each failure <strong>is recorded</strong> with its error, attempt count,
 *       and backoff delay until the next retry. Previously, exceptions were swallowed silently.</li>
 *   <li>Publishing occurs <strong>outside</strong> the business transaction that committed the change,
 *       and after the outbox transaction has closed. A publisher failure cannot roll back an already
 *       committed business state transition.</li>
 * </ul>
 */
public class OutboxProcessorService implements ProcessOutboxMessagesUseCase {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessorService.class);

    private final StockOutboxRepositoryPort outboxRepositoryPort;
    private final EventPublisherPort eventPublisherPort;
    private final UnitOfWork unitOfWork;
    private final int maxAttempts;
    private final Duration maxDelay;

    public OutboxProcessorService(StockOutboxRepositoryPort outboxRepositoryPort,
                                  EventPublisherPort eventPublisherPort,
                                  UnitOfWork unitOfWork,
                                  int maxAttempts,
                                  Duration maxDelay) {
        this.outboxRepositoryPort = outboxRepositoryPort;
        this.eventPublisherPort = eventPublisherPort;
        this.unitOfWork = unitOfWork;
        this.maxAttempts = maxAttempts;
        this.maxDelay = maxDelay;
    }

    @Override
    public int processUnpublishedMessages(int batchSize) {
        // Row claiming and locking occurs within its own transaction,
        // which closes upon exiting this block. Publishing is therefore
        // decoupled from any database transaction.
        List<StockOutboxMessage> messages = unitOfWork.callInTransaction(
                () -> outboxRepositoryPort.findPending(Instant.now(), batchSize));

        int published = 0;
        for (StockOutboxMessage message : messages) {
            try {
                eventPublisherPort.publish(message);
                message.markAsPublished();
                unitOfWork.runInTransaction(() -> outboxRepositoryPort.save(message));
                published++;
            } catch (Exception e) {
                // A failed message does not halt the batch: it is recorded and rescheduled,
                // allowing subsequent messages to proceed.
                log.warn("[OUTBOX-RELAY] No se pudo publicar el evento {} ({}). Intento {}/{}: {}",
                        message.getEventType(), message.getId(), message.getAttempts() + 1, maxAttempts, e.getMessage());
                message.recordFailure(e.getMessage(), maxAttempts, maxDelay);
                unitOfWork.runInTransaction(() -> outboxRepositoryPort.save(message));
            }
        }
        return published;
    }
}
