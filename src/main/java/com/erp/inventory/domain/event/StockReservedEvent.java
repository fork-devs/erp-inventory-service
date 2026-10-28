package com.erp.inventory.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Stock domain event: workshop reserved units for a work order.
 *
 * <p>The event identifier travels within the message and is not regenerated across retries,
 * enabling consumers to deduplicate: delivery is at-least-once, and the event identifier
 * enables effective deduplication.
 */
public class StockReservedEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID tenantId;
    private final UUID movementId;
    private final UUID sparePartId;
    private final UUID branchId;
    private final int quantity;
    private final UUID workOrderId;
    private final Instant occurredAt;

    public StockReservedEvent(UUID tenantId, UUID movementId, UUID sparePartId, UUID branchId,
                              int quantity, UUID workOrderId) {
        this.eventId = UUID.randomUUID();
        this.tenantId = tenantId;
        this.movementId = movementId;
        this.sparePartId = sparePartId;
        this.branchId = branchId;
        this.quantity = quantity;
        this.workOrderId = workOrderId;
        this.occurredAt = Instant.now();
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "StockReserved";
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getMovementId() {
        return movementId;
    }

    public UUID getSparePartId() {
        return sparePartId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public int getQuantity() {
        return quantity;
    }

    public UUID getWorkOrderId() {
        return workOrderId;
    }
}
