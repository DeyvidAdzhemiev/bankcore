package com.bankcore.account.api.dto;

import com.bankcore.account.domain.AccountType;
import com.bankcore.common.util.Currency;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/accounts}.
 *
 * <p>The account number and initial balance are generated server-side;
 * the client only specifies ownership, product type, and currency.
 */
public record CreateAccountRequest(

        @NotNull(message = "userId is required")
        UUID userId,

        @NotNull(message = "type is required")
        AccountType type,

        @NotNull(message = "currency is required")
        Currency currency
) {}