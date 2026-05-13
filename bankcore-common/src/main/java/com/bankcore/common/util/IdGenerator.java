package com.bankcore.common.util;

import java.util.UUID;

/**
 * Utility for generating unique identifiers across all BankCore services.
 */
public final class IdGenerator {

    private IdGenerator() {
        throw new UnsupportedOperationException("Utility class — do not instantiate");
    }

    public static UUID generate() {
        return UUID.randomUUID();
    }

    public static String generateAsString() {
        return UUID.randomUUID().toString();
    }
}