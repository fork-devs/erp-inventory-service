package com.erp.inventory.domain.port;

/**
 * Transactional boundary abstraction for business operations.
 *
 * <p>A domain abstraction free of framework dependencies, allowing the application
 * layer to enforce atomic commits across multiple repository writes regardless
 * of the underlying transaction management implementation.
 *
 * <p>Without this boundary, individual repository calls could each execute in separate
 * transactions, breaking atomicity across dependent operations.
 */
public interface UnitOfWork {

    void runInTransaction(Runnable work);

    <T> T callInTransaction(java.util.function.Supplier<T> work);
}
