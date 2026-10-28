package com.erp.inventory.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Boots an embedded PostgreSQL process for integration tests that require verifying
 * actual PostgreSQL behavior (Row-Level Security, constraints, Flyway migrations).
 */
public final class EmbeddedPostgresSupport {

    /** Application runtime role: does not own database tables. */
    public static final String APP_ROLE = "erp_inventory_app";
    public static final String APP_PASSWORD = "erp_inventory_app_test_password";

    /** Platform maintenance role for administrative jobs. */
    public static final String PLATFORM_ROLE = "erp_inventory_platform";
    public static final String PLATFORM_PASSWORD = "erp_inventory_platform_test_password";

    public static final String OWNER_PASSWORD = "erp_inventory_owner_test_password";
    public static final String DATABASE = "postgres";

    private static EmbeddedPostgres postgres;
    private static String jdbcUrl;
    private static String ownerUser;

    private EmbeddedPostgresSupport() {
    }

    public static synchronized void start() {
        if (postgres != null) {
            return;
        }
        try {
            postgres = EmbeddedPostgres.builder()
                    .setServerConfig("listen_addresses", "localhost")
                    .setServerConfig("fsync", "off")
                    .setServerConfig("full_page_writes", "off")
                    .setServerConfig("synchronous_commit", "off")
                    .start();

            ownerUser = resolveOwnerUser();
            jdbcUrl = postgres.getJdbcUrl(DATABASE, ownerUser).split("[?]")[0];

            try (Connection connection = postgres.getPostgresDatabase().getConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("ALTER ROLE \"" + ownerUser + "\" WITH PASSWORD '" + OWNER_PASSWORD + "'");
            }
            // Application and platform roles are provisioned by Flyway migration scripts,
            // which grant appropriate permissions on created tables.
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("No se pudo arrancar PostgreSQL embebido para los tests", e);
        }
    }

    public static synchronized void stop() {
        if (postgres == null) {
            return;
        }
        try {
            postgres.close();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo detener PostgreSQL embebido", e);
        } finally {
            postgres = null;
            jdbcUrl = null;
            ownerUser = null;
        }
    }

    public static String jdbcUrl() {
        start();
        return jdbcUrl;
    }

    public static String ownerUser() {
        start();
        return ownerUser;
    }

    public static String ownerPassword() {
        return OWNER_PASSWORD;
    }

    public static String appRole() {
        return APP_ROLE;
    }

    public static String appPassword() {
        return APP_PASSWORD;
    }

    public static String platformRole() {
        return PLATFORM_ROLE;
    }

    public static String platformPassword() {
        return PLATFORM_PASSWORD;
    }

    public static Connection openOwnerConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(jdbcUrl(), ownerUser(), OWNER_PASSWORD);
    }

    public static Connection openAppConnection() throws SQLException {
        return java.sql.DriverManager.getConnection(jdbcUrl(), APP_ROLE, APP_PASSWORD);
    }

    private static String resolveOwnerUser() throws SQLException {
        try (Connection connection = postgres.getPostgresDatabase().getConnection()) {
            return connection.getMetaData().getUserName();
        }
    }
}
