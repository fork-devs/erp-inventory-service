package com.erp.inventory.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Guard component verifying that an active transactional boundary is present.
 *
 * <p>Persistence adapters join the transaction opened by the application layer.
 * This guard ensures that invocations outside that boundary fail explicitly,
 * preventing auto-committed operations that would violate application-level atomicity.
 */
@Component
public class TransactionGuard {

    public void requireActiveTransaction(String operacion) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new MissingTransactionBoundaryException(operacion);
        }
    }
}
