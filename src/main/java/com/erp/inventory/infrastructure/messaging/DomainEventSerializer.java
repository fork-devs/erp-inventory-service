package com.erp.inventory.infrastructure.messaging;

import com.erp.inventory.domain.event.DomainEvent;
import com.erp.inventory.domain.event.StockLowEvent;
import com.erp.inventory.domain.event.StockReleasedEvent;
import com.erp.inventory.domain.event.StockReservedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Serializes domain events to JSON payloads.
 *
 * <p>Uses a standard Jackson serializer to properly escape characters, avoiding
 * format string vulnerabilities and malformed JSON payloads.
 *
 * <p>The event identifier is assigned by the domain event itself, guaranteeing that
 * the same identifier accompanies the event across all outbox delivery retries.
 */
@Component
public class DomainEventSerializer {

    private final ObjectMapper objectMapper;

    public DomainEventSerializer() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public DomainEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(DomainEvent event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("eventId", event.getEventId().toString());
        node.put("eventType", event.getEventType());
        node.put("occurredAt", DateTimeFormatter.ISO_INSTANT.format(event.getOccurredAt()));

        if (event instanceof StockReservedEvent reservado) {
            node.put("tenantId", reservado.getTenantId().toString());
            node.put("movementId", reservado.getMovementId().toString());
            node.put("sparePartId", reservado.getSparePartId().toString());
            node.put("branchId", reservado.getBranchId().toString());
            node.put("quantity", reservado.getQuantity());
            node.put("workOrderId", reservado.getWorkOrderId() == null ? null : reservado.getWorkOrderId().toString());
        } else if (event instanceof StockReleasedEvent liberado) {
            node.put("tenantId", liberado.getTenantId().toString());
            node.put("movementId", liberado.getMovementId().toString());
            node.put("sparePartId", liberado.getSparePartId().toString());
            node.put("branchId", liberado.getBranchId().toString());
            node.put("quantity", liberado.getQuantity());
            node.put("workOrderId", liberado.getWorkOrderId() == null ? null : liberado.getWorkOrderId().toString());
        } else if (event instanceof StockLowEvent bajo) {
            node.put("tenantId", bajo.getTenantId().toString());
            node.put("sparePartId", bajo.getSparePartId().toString());
            node.put("branchId", bajo.getBranchId().toString());
            node.put("availableStock", bajo.getAvailableStock());
            node.put("minStock", bajo.getMinStock());
        }

        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el evento de dominio", e);
        }
    }
}
