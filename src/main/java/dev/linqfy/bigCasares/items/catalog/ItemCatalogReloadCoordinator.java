package dev.linqfy.bigCasares.items.catalog;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ItemCatalogReloadCoordinator {

    private final AtomicBoolean running = new AtomicBoolean();

    public ItemCatalogReloadResult execute(ItemCatalogReloadOperation operation) {
        Objects.requireNonNull(operation, "operation");
        if (!running.compareAndSet(false, true)) {
            return new ItemCatalogReloadResult(
                ItemCatalogReloadStatus.ALREADY_RUNNING, Duration.ZERO,
                operation.activeCatalog().orElse(null), null, null, null
            );
        }
        long started = System.nanoTime();
        CustomItemCatalog oldCatalog = operation.activeCatalog().orElse(null);
        try {
            CustomItemCatalog candidate = operation.loadCandidate();
            if (oldCatalog != null && oldCatalog.revision().equals(candidate.revision())) {
                return new ItemCatalogReloadResult(
                    ItemCatalogReloadStatus.NO_OP, elapsed(started), oldCatalog, oldCatalog, null, null
                );
            }
            ItemReconciliationResult reconciliation = operation.commit(candidate);
            return new ItemCatalogReloadResult(
                ItemCatalogReloadStatus.SUCCESS, elapsed(started), oldCatalog, candidate, reconciliation, null
            );
        } catch (Throwable failure) {
            return new ItemCatalogReloadResult(
                ItemCatalogReloadStatus.FAILED, elapsed(started), oldCatalog, null, null, failure
            );
        } finally {
            running.set(false);
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private static Duration elapsed(long started) {
        return Duration.ofNanos(Math.max(0L, System.nanoTime() - started));
    }
}
