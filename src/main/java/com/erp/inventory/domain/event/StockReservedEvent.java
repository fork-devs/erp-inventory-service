package com.erp.inventory.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de stock: el taller reservo unidades para una orden de trabajo.
 *
 * <p>El identificador del evento viaja en el mensaje y no se regenera en cada
 * reintento, de modo que un consumidor pueda deduplicar: la entrega es al
 * menos una vez, y el identificador es lo que hace la deduplicacion posible.
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
