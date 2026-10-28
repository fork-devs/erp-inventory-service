package com.erp.inventory.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests against embedded PostgreSQL.
 *
 * <p>Boots an embedded PostgreSQL instance and connects the application using the
 * runtime role {@code erp_inventory_app}, which does not own the schema tables.
 * Flyway migrations are executed by the schema owner.
 */
@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        EmbeddedPostgresSupport.start();

        registry.add("spring.datasource.url", EmbeddedPostgresSupport::jdbcUrl);
        registry.add("spring.datasource.username", EmbeddedPostgresSupport::appRole);
        registry.add("spring.datasource.password", EmbeddedPostgresSupport::appPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.flyway.url", EmbeddedPostgresSupport::jdbcUrl);
        registry.add("spring.flyway.user", EmbeddedPostgresSupport::ownerUser);
        registry.add("spring.flyway.password", EmbeddedPostgresSupport::ownerPassword);
        registry.add("spring.flyway.placeholders.appRolePassword", EmbeddedPostgresSupport::appPassword);
        registry.add("spring.flyway.placeholders.platformRolePassword",
                EmbeddedPostgresSupport::platformPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");

        // The service validates JWT tokens against the JWKS endpoint of the credentials issuer.
        // Tests boot an embedded test JWKS server so that issued tokens are genuinely signed.
        registry.add("erp.auth.jwks-url", TestJwksServer::jwksUrl);

        // Platform role configuration for background maintenance operations.
        registry.add("erp.platform-role.user", EmbeddedPostgresSupport::platformRole);
        registry.add("erp.platform-role.password", EmbeddedPostgresSupport::platformPassword);

    }
}
