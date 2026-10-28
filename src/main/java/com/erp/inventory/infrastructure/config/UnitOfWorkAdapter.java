package com.erp.inventory.infrastructure.config;

import com.erp.inventory.domain.port.UnitOfWork;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Implementation of the domain transactional boundary using {@link TransactionTemplate}.
 *
 * <p>The template employs default propagation (REQUIRED): if an active transaction
 * already exists, the operation joins it rather than initiating a new one. This ensures
 * that nested demarcations participate in the same atomic transaction.
 */
@Component
public class UnitOfWorkAdapter implements UnitOfWork {

    private final TransactionTemplate transactionTemplate;

    public UnitOfWorkAdapter(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void runInTransaction(Runnable work) {
        transactionTemplate.executeWithoutResult(status -> work.run());
    }

    @Override
    public <T> T callInTransaction(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }
}
