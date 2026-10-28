package com.erp.inventory.domain.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pending message in the outbox queue waiting to be published to the event bus.
 *
 * <p>The event identifier is embedded in the message and is not regenerated across retries,
 * enabling consumers to deduplicate: delivery guarantees are at-least-once, and the persistent
 * event ID provides idempotency on consumption.
 */
public class StockOutboxMessage implements Serializable {

    /**
     * Exponential backoff delays between retries in seconds, capping at 24 hours
     * to accommodate temporary provider degradation windows.
     */
    private static final long[] BACKOFF_SECONDS = {5, 30, 300, 1800, 7200, 28800, 86400};

    private final UUID id;
    private final UUID eventId;
    private final String aggregateType;
    private final UUID aggregateId;
    private final UUID tenantId;
    private final String eventType;
    private final String payload;
    private int attempts;
    private Instant nextAttemptAt;
    private String lastError;
    private StockOutboxStatus status;
    private final Instant createdAt;

    public StockOutboxMessage(UUID id, UUID eventId, String aggregateType, UUID aggregateId, UUID tenantId,
                               String eventType, String payload, int attempts, Instant nextAttemptAt,
                               String lastError, StockOutboxStatus status, Instant createdAt) {
        if (id == null || eventId == null || aggregateType == null || aggregateId == null || tenantId == null
                || eventType == null || payload == null || status == null) {
            throw new IllegalArgumentException("Campos obligatorios del OutboxMessage no pueden ser nulos");
        }
        this.id = id;
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.tenantId = tenantId;
        this.eventType = eventType;
        this.payload = payload;
        this.attempts = attempts;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = lastError;
        this.status = status;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StockOutboxMessage of(UUID aggregateId, UUID tenantId, UUID eventId, String eventType, String payload) {
        return new StockOutboxMessage(
                UUID.randomUUID(),
                eventId,
                "TENANT",
                aggregateId,
                tenantId,
                eventType,
                payload,
                0,
                Instant.now(),
                null,
                StockOutboxStatus.PENDING,
                Instant.now());
    }

    public void markAsPublished() {
        this.status = StockOutboxStatus.PUBLISHED;
        this.lastError = null;
    }

    /**
     * Records a publishing failure: increments attempt count, saves error detail,
     * and schedules the next retry using exponential backoff. Upon reaching max attempts,
     * the message transitions to DEAD_LETTER and stops auto-retrying while remaining persisted.
     */
    public void recordFailure(String error, int maxAttempts, Duration maxDelay) {
        this.attempts++;
        this.lastError = error;

        if (this.attempts >= maxAttempts) {
            this.status = StockOutboxStatus.DEAD_LETTER;
            this.nextAttemptAt = null;
            return;
        }

        this.status = StockOutboxStatus.PENDING;
        this.nextAttemptAt = Instant.now().plus(nextDelay(maxDelay));
    }

    /**
     * Re-queues the message as PENDING with attempts reset to 0 for manual retry.
     */
    public void reactivate() {
        this.attempts = 0;
        this.lastError = null;
        this.status = StockOutboxStatus.PENDING;
        this.nextAttemptAt = Instant.now();
    }

    /**
     * Computes the delay until the next retry attempt based on exponential backoff, capped by maxDelay.
     */
    public Duration nextDelay(Duration maxDelay) {
        int index = Math.min(Math.max(attempts - 1, 0), BACKOFF_SECONDS.length - 1);
        Duration delay = Duration.ofSeconds(BACKOFF_SECONDS[index]);
        return delay.compareTo(maxDelay) > 0 ? maxDelay : delay;
    }

    public boolean isDeadLettered() {
        return status == StockOutboxStatus.DEAD_LETTER;
    }

    public boolean isPublished() {
        return status == StockOutboxStatus.PUBLISHED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public StockOutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StockOutboxMessage that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
