package com.bankcore.common.exception;

/**
 * Thrown when a transaction submission is rejected because a transaction with the
 * same idempotency key already exists.
 */
public class DuplicateTransactionException extends BankCoreException {

    private static final String ERROR_CODE = "DUPLICATE_TRANSACTION";

    public DuplicateTransactionException(String idempotencyKey) {
        super(ERROR_CODE,
                "A transaction with idempotency key '" + idempotencyKey + "' already exists. "
                        + "The original result should be returned to the caller.");
    }
}