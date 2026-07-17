package dev.linqfy.bigCasares.items.catalog;

import java.time.Duration;
import java.util.Objects;

public record ItemCatalogReloadResult(
    ItemCatalogReloadStatus status,
    Duration duration,
    CustomItemCatalog oldCatalog,
    CustomItemCatalog newCatalog,
    ItemReconciliationResult reconciliation,
    Throwable failure
) {

    public ItemCatalogReloadResult {
        status = Objects.requireNonNull(status, "status");
        duration = Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        if (status == ItemCatalogReloadStatus.FAILED && failure == null) {
            throw new IllegalArgumentException("failed reload results need a failure");
        }
        if (status != ItemCatalogReloadStatus.FAILED && failure != null) {
            throw new IllegalArgumentException("successful reload results must not contain a failure");
        }
    }
}
