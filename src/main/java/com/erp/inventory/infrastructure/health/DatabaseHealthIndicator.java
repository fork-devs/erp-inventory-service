package com.erp.inventory.infrastructure.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Live database health verification indicator.
 *
 * <p>Executes an actual query over a borrowed connection rather than simply checking
 * configuration. This ensures that broken connection pools or database outages are
 * detected accurately by orchestration and load-balancing components.
 */
@Component("inventoryDatabase")
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            Integer resultado = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (resultado != null && resultado == 1) {
                return Health.up().withDetail("comprobacion", "consulta ejecutada").build();
            }
            return Health.down().withDetail("comprobacion", "la consulta no devolvio lo esperado").build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
