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
 * Serializa un evento de dominio a JSON.
 *
 * <p>El payload lo produce un serializador de verdad: el anterior se
 * construia con {@code String.format}, escapando solo las comillas dobles, y
 * un nombre con una barra invertida o un salto de linea producia un JSON
 * INVALIDO, imposible de parsear para cualquier consumidor.
 *
 * <p>El identificador del evento lo genera el evento, no el serializador, de
 * modo que el mismo identificador acompanie al mensaje en todos sus reintentos
 * y el consumidor pueda deduplicar.
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
