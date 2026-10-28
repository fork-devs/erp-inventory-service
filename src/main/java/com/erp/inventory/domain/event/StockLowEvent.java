package com.erp.inventory.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Stock domain event: available stock for a spare part has dropped below
 * its minimum reorder threshold.
 *
 * <p>The notification records stock level at the moment of the movement: subsequent
 * threshold adjustments do not alter historical notifications.
 */
public class StockLowEvent implements DomainEvent {

    private final UUID eventId;
    private final UUID tenantId;
    private final UUID sparePartId;
    private final UUID branchId;
    private final int availableStock;
    private final int minStock;
    private final Instant occurredAt;

    public StockLowEvent(UUID tenantId, UUID sparePartId, UUID branchId,
                          int availableStock, int minStock) {
        this.eventId = UUID.randomUUID();
        this.tenantId = tenantId;
        this.sparePartId = sparePartId;
        this.branchId = branchId;
        this.availableStock = availableStock;
        this.minStock = minStock;
        this.occurredAt = Instant.now();
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "StockLow";
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getSparePartId() {
        return sparePartId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public int getMinStock() {
        return minStock;
    }
}
