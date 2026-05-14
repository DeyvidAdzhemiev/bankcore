package com.bankcore.account.domain;

/**
 * Lifecycle status of a bank account.
 *
 * <p>Stored as a string in the {@code accounts.status} column and validated at the DB level
 * by a {@code CHECK} constraint in the Flyway migration.
 *
 * <p>Valid transitions:
 * <pre>
 *   ACTIVE ──▶ FROZEN ──▶ ACTIVE
 *   ACTIVE ──▶ CLOSED
 *   FROZEN ──▶ CLOSED
 * </pre>
 */
public enum AccountStatus {

    ACTIVE,
    FROZEN,
    CLOSED
}