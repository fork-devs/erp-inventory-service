package com.erp.inventory.infrastructure.persistence;

/**
 * Infrastructure exception thrown when a persistence operation is invoked
 * without an active transactional boundary.
 *
 * <p>Persistence adapters do not initiate their own transactions; they participate
 * in the transaction opened by the application layer. Without an active boundary,
 * writes would commit separately, violating atomicity requirements.
 */
public class MissingTransactionBoundaryException extends RuntimeException {

    public MissingTransactionBoundaryException(String operacion) {
        super("La operacion de persistencia '" + operacion
                + "' requiere un limite transaccional abierto por la capa de aplicacion. "
                + "Los adaptadores se unen a la transaccion en curso; no abren una propia.");
    }
}
