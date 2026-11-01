package com.erp.inventory.infrastructure.persistence.rls;

import com.erp.inventory.domain.exception.TenantContextMissingException;
import com.erp.inventory.infrastructure.config.PersistenceConfig;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Sets the tenant context on the PostgreSQL session so that Row-Level Security (RLS)
 * policies can isolate rows per workshop tenant.
 *
 * <p>Isolation fails closed: if no active transaction exists, {@code SET LOCAL} would have
 * no effect and queries would execute unfiltered, so the operation is rejected explicitly.
 *
 * <p>The tenant context is determined strictly by the authenticated principal, never by
 * the target entity row, guaranteeing secure tenant boundary enforcement.
 */
@Aspect
@Component
@Order(PersistenceConfig.TENANT_RLS_ASPECT_ORDER)
public class PostgresTenantRlsAspect {

    private final EntityManager entityManager;

    @Autowired
    public PostgresTenantRlsAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Enforces that every persistence operation executes within an active transaction:
     * outside a transaction boundary, session tenant context cannot be reliably scoped.
     */
    @Before("execution(* com.erp.inventory.infrastructure.persistence.adapter..*(..))")
    public void requireActiveTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new TenantContextMissingException(
                    "La operacion de persistencia debe ejecutarse dentro de una transaccion: "
                    + "fuera de ella no puede garantizarse el aislamiento por taller.");
        }
    }

    /**
     * Sets the local tenant configuration variable for tenant-scoped repositories.
     *
     * <p>The value is interpolated into the statement because PostgreSQL's {@code SET LOCAL}
     * does not support parameterized query placeholders. The tenant identifier is a validated UUID,
     * preventing SQL injection vectors.
     */
    @Before("execution(* com.erp.inventory.infrastructure.persistence.adapter.BranchPersistenceAdapter.*(..))"
            + " || execution(* com.erp.inventory.infrastructure.persistence.adapter.SparePartPersistenceAdapter.*(..))"
            + " || execution(* com.erp.inventory.infrastructure.persistence.adapter.StockMovementPersistenceAdapter.*(..))"
            + " || execution(* com.erp.inventory.infrastructure.persistence.adapter.StockLotPersistenceAdapter.*(..))"
            + " || execution(* com.erp.inventory.infrastructure.persistence.adapter.ProductPersistenceAdapter.*(..))"
            + " || execution(* com.erp.inventory.infrastructure.persistence.adapter.ProductBatchPersistenceAdapter.*(..))")
    public void applyTenantContext() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException(
                    "La operacion requiere un contexto de taller establecido y no lo tiene.");
        }
        entityManager.createNativeQuery("SET LOCAL app.tenant_id = '" + tenantId + "'").executeUpdate();
    }
}
