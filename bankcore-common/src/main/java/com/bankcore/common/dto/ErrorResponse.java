package com.bankcore.common.dto;

import java.time.Instant;

/**
 * Standardised error response body returned by all BankCore REST APIs.
 *
 * <p>Example JSON:
 * <pre>{@code
 * {
 *   "timestamp": "2026-04-02T10:15:30.123Z",
 *   "status":    404,
 *   "error":     "ACCOUNT_NOT_FOUND",
 *   "message":   "Account not found: 3fa85f64-...",
 *   "path":      "/api/v1/accounts/3fa85f64-..."
 * }
 * }</pre>
 */
public record ErrorResponse(

        Instant timestamp,

        int status,

        String error,

        String message,

        String path
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }

    public static ErrorResponse of(int status, String message, String path) {
        String error = status >= 500 ? "INTERNAL_ERROR" : "CLIENT_ERROR";
        return of(status, error, message, path);
    }
}