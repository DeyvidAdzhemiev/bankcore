package com.bankcore.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object representing a monetary amount in a specific {@link Currency}.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>All arithmetic uses {@link RoundingMode#HALF_EVEN} (banker's rounding) at scale 2.</li>
 *   <li>Amounts are always {@code >= 0}. A debit that would produce a negative result is
 *       rejected at the service layer, not here — Money itself only prevents construction
 *       of negative amounts.</li>
 *   <li>Currency mismatch on arithmetic operations throws {@link IllegalArgumentException}
 *       immediately rather than silently converting, making bugs visible at development time.</li>
 *   <li>The canonical record constructor normalises scale so that {@code Money.of("1.1", USD)}
 *       and {@code Money.of("1.10", USD)} are {@link #equals equal}.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Money price  = Money.of("49.99", Currency.USD);
 * Money tax    = price.multiply(new BigDecimal("0.08"));
 * Money total  = price.add(tax);
 * }</pre>
 */
public record Money(BigDecimal amount, Currency currency) {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Monetary amount must be >= 0, got: " + amount.toPlainString());
        }
        amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money of(String amount, Currency currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        return new Money(new BigDecimal(amount), currency);
    }

    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Subtraction would produce a negative amount: " + this + " - " + other);
        }
        return new Money(result, this.currency);
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
        if (factor.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Multiplication factor must be >= 0, got: " + factor.toPlainString());
        }
        return new Money(
                this.amount.multiply(factor).setScale(SCALE, ROUNDING),
                this.currency);
    }

    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isGreaterThanOrEqualTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) >= 0;
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: cannot operate on " + this.currency
                            + " and " + other.currency);
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.name();
    }
}