package com.bankcore.common.exception;

/**
 * Base runtime exception for all domain-level errors in the BankCore platform.
 *
 * <p>Every service-specific exception must extend this class so that
 * {@code GlobalExceptionHandler} can catch the base type as a fallback, and so that
 * error codes are always present in the response.
 *
 */
public class BankCoreException extends RuntimeException {

    private final String errorCode;

    public BankCoreException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BankCoreException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}