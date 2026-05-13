package com.bankcore.common.event;

import com.bankcore.common.util.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by {@code transaction-service} when a new transaction is initiated
 * and written to the outbox table, before fraud checks or settlement.
 *
 * <p><strong>Topic:</strong> {@code transaction.created}
 * <br><strong>Consumers:</strong> {@code fraud-service}, {@code audit-service}
 *
 * <p>All fields are mandatory. {@code destAccountId} is null for external transfers
 * (card payments, withdrawals) — consumers must handle null safely.
 */
public record TransactionCreatedEvent(

        UUID transactionId,

        UUID sourceAccountId,

        UUID destAccountId,

        BigDecimal amount,

        Currency currency,

        String type,

        String idempotencyKey,

        Instant occurredAt
) {}