package com.erp.inventory;

import com.erp.inventory.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * El contexto de la aplicacion arranca contra PostgreSQL real, con las
 * migraciones aplicadas y el esquema validado por el ORM.
 *
 * <p>Antes el test usaba H2 con un esquema derivado de las entidades: la
 * migracion nunca se ejecutaba y el arranque no demostraba nada sobre el
 * esquema real.
 */
class ErpInventoryServiceApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    void contextLoads() {
    }
}
