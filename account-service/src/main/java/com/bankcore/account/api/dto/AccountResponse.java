package com.bankcore.account.api.dto;

import com.bankcore.account.domain.AccountStatus;
import com.bankcore.account.domain.AccountType;
import com.bankcore.common.util.Currency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only projection of an {@code Account} returned by the REST API.
 *
 * <p>Using a record ensures this DTO is immutable and cannot accidentally be
 * used to mutate the domain entity. Jackson serialises records correctly out of
 * the box with Spring Boot 3.x.
 */
public record AccountResponse(

        UUID id,
        UUID userId,
        String accountNumber,
        AccountType type,
        Currency currency,
        BigDecimal balance,
        BigDecimal availableBalance,
        AccountStatus status,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}