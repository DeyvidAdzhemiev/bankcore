package com.bankcore.account.api.dto;

import com.bankcore.common.util.Currency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for debit ({@code POST /api/v1/accounts/{id}/debit}) and
 * credit ({@code POST /api/v1/accounts/{id}/credit}) operations.
 */
public record MoneyOperationRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be at least 0.01")
        BigDecimal amount,

        @NotNull(message = "currency is required")
        Currency currency
) {}