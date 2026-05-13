package com.bankcore.common.util;

/**
 * Supported currencies in the BankCore platform.
 *
 * <p>Using a bounded enum rather than {@link java.util.Currency} keeps the domain
 * explicit and avoids accidentally accepting unsupported currency codes at runtime.
 * Add new values here when onboarding a new currency — the compiler will then
 * surface every switch expression that needs updating.
 */
public enum Currency {

    USD("US Dollar"),
    EUR("Euro"),
    GBP("British Pound Sterling"),
    PLN("Polish Zloty"),
    TRY("Turkish Lira");

    private final String displayName;

    Currency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}