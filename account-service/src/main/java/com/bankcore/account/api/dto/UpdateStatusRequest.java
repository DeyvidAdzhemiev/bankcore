package com.bankcore.account.api.dto;

import com.bankcore.account.domain.AccountStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PATCH /api/v1/accounts/{id}/status}.
 */
public record UpdateStatusRequest(

        @NotNull(message = "status is required")
        AccountStatus status
) {}