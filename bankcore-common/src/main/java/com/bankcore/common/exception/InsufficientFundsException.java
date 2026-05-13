package com.bankcore.common.exception;

import java.util.UUID;

/**
 * Thrown when a debit operation is requested but the account's available balance
 * is lower than the requested amount.
 */
public class InsufficientFundsException extends BankCoreException {

    private static final String ERROR_CODE = "INSUFFICIENT_FUNDS";

    public InsufficientFundsException(UUID accountId, String requested, String available) {
        super(ERROR_CODE,
                "Insufficient funds in account " + accountId
                        + ": requested " + requested
                        + ", available " + available);
    }
}