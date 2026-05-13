package com.bankcore.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Utility for date/time conversions in the BankCore platform.
 *
 * <p><strong>Convention:</strong> all persistence and inter-service communication use
 * {@link Instant} (UTC epoch-based). {@link LocalDateTime} conversions are provided
 * only for display layers or legacy integration points that cannot accept {@code Instant}.
 *
 * <p>This class has no state and cannot be instantiated.
 */
public final class DateUtils {

    private DateUtils() {
        throw new UnsupportedOperationException("Utility class — do not instantiate");
    }

    public static Instant nowUtc() {
        return Instant.now();
    }

    public static LocalDateTime toLocalDateTime(Instant instant) {
        if (instant == null) {
            throw new IllegalArgumentException("instant must not be null");
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static Instant toInstant(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            throw new IllegalArgumentException("localDateTime must not be null");
        }
        return localDateTime.toInstant(ZoneOffset.UTC);
    }
}