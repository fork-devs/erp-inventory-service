package com.erp.inventory.infrastructure.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

/**
 * Disponibilidad del servicio.
 *
 * <p>Comprueba de verdad su dependencia: la base de datos. No se limita a decir
 * que esta configurada: se consulta de verdad, de modo que un pool roto se
 * distingue de un servicio sano.
 *
 * <p>El servicio informa de NO disponible cuando la comprobacion falla: es lo
 * que un orquestador necesita para dejar de mandarle trafico.
 */
@Component("inventoryReadiness")
public class ReadinessIndicator implements HealthIndicator {

    private final DatabaseHealthIndicator database;

    public ReadinessIndicator(DatabaseHealthIndicator database) {
        this.database = database;
    }

    @Override
    public Health health() {
        Health saludBaseDeDatos = database.health();

        Health.Builder builder = saludBaseDeDatos.getStatus() == Status.DOWN
                ? Health.down()
                : Health.up();

        return builder
                .withDetail("base_de_datos", saludBaseDeDatos.getStatus().getCode())
                .build();
    }
}
