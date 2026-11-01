package com.erp.inventory;

import com.erp.inventory.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the Spring application context boots against a live embedded PostgreSQL instance,
 * with Flyway migrations applied and the schema validated by Hibernate.
 */
class ErpInventoryServiceApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
    }
}
