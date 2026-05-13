package com.bankcore.common.event;

import com.bankcore.common.util.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by {@code transaction-service} when a transaction reaches a terminal state:
 * either {@code COMPLETED} (settlement succeeded) or {@code FAILED} (settlement failed
 * after all retries, or rejected by fraud-service).
 *
 * <p><strong>Topic:</strong> {@code transaction.completed}
 * <br><strong>Consumers:</strong> {@code notification-service}, {@code audit-service}
 */
public record TransactionCompletedEvent(

        UUID transactionId,

        UUID sourceAccountId,

        UUID destAccountId,

        BigDecimal amount,

        Currency currency,

        String status,

        String failureReason,

        Instant completedAt
) {}