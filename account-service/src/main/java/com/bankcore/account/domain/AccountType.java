package com.bankcore.account.domain;

/**
 * Product type of a bank account.
 *
 * <p>Stored as a string in the {@code accounts.type} column and validated at the DB level
 * by a {@code CHECK} constraint in the Flyway migration.
 */
public enum AccountType {

    CHECKING,
    SAVINGS,
    CREDIT
}