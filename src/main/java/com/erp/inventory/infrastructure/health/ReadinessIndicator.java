package com.erp.inventory.infrastructure.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

/**
 * Service readiness indicator.
 *
 * <p>Validates core infrastructure dependencies (e.g., database connectivity)
 * by executing actual operations, ensuring broken connection pools are accurately distinguished.
 *
 * <p>Reports OUT_OF_SERVICE / DOWN when checks fail, signaling orchestrators to stop routing traffic.
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
