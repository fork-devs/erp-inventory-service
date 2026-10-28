package com.erp.inventory.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration for transactional demarcation and advice precedence.
 *
 * <p>Advice ordering is critical: Spring's transaction interceptor and the
 * tenant context aspect compete for execution precedence. Without explicit ordering,
 * the order is indeterminate, which could cause {@code SET LOCAL} to execute OUTSIDE
 * an active transaction where it has no effect, allowing queries to run without isolation filters.
 *
 * <p>By placing the transaction interceptor at order 0 and the RLS aspect at order 1,
 * the tenant aspect is guaranteed to execute INSIDE the transaction boundary where
 * {@code SET LOCAL} is scoped and effective.
 */
@Configuration
@EnableTransactionManagement(order = 0)
public class PersistenceConfig {

    /**
     * Precedence order for the Row-Level Security tenant aspect. In Spring AOP,
     * a LOWER order value indicates higher precedence (OUTER advice). The tenant RLS
     * aspect must be INNER to the transaction interceptor, hence a higher order value.
     */
    public static final int TENANT_RLS_ASPECT_ORDER = 1;
}
