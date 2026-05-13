package com.bankcore.common.exception;

import java.util.UUID;

/**
 * Thrown when an account is looked up by ID but does not exist in the database.
 *
 * <p>Maps to HTTP 404 Not Found.
 */
public class AccountNotFoundException extends BankCoreException {

    private static final String ERROR_CODE = "ACCOUNT_NOT_FOUND";

    public AccountNotFoundException(UUID accountId) {
        super(ERROR_CODE, "Account not found: " + accountId);
    }

    public AccountNotFoundException(String accountNumber) {
        super(ERROR_CODE, "Account not found for account number: " + accountNumber);
    }
}