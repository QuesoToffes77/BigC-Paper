package dev.linqfy.bigCasares.items.catalog;

public record ItemReconciliationResult(int refreshed, int legacyMarked, int ignored, int remaining) {

    public ItemReconciliationResult {
        if (refreshed < 0 || legacyMarked < 0 || ignored < 0 || remaining < 0) {
            throw new IllegalArgumentException("reconciliation counts must not be negative");
        }
    }
}
