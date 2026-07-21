package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCatalogReloadCoordinatorTest {

    @Test
    void reportsNoOpWithoutCommittingAnIdenticalCatalog() {
        CustomItemCatalog catalog = catalog("Manzana de Cobre");
        FakeOperation operation = new FakeOperation(catalog, catalog);

        ItemCatalogReloadResult result = new ItemCatalogReloadCoordinator().execute(operation);

        assertEquals(ItemCatalogReloadStatus.NO_OP, result.status());
        assertEquals(0, operation.commits);
    }

    @Test
    void retainsOldCatalogWhenCandidateLoadingFails() {
        CustomItemCatalog oldCatalog = catalog("Manzana de Cobre");
        FakeOperation operation = new FakeOperation(oldCatalog, null);
        operation.failure = new IllegalArgumentException("invalid definition");

        ItemCatalogReloadResult result = new ItemCatalogReloadCoordinator().execute(operation);

        assertEquals(ItemCatalogReloadStatus.FAILED, result.status());
        assertEquals(oldCatalog, result.oldCatalog());
        assertEquals(0, operation.commits);
        assertTrue(result.failure().getMessage().contains("invalid definition"));
    }

    @Test
    void rejectsAConcurrentReload() throws Exception {
        CustomItemCatalog oldCatalog = catalog("Manzana de Cobre");
        CustomItemCatalog candidate = catalog("Copper Apple");
        FakeOperation operation = new FakeOperation(oldCatalog, candidate);
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        operation.beforeLoad = () -> {
            loading.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        };
        ItemCatalogReloadCoordinator coordinator = new ItemCatalogReloadCoordinator();
        AtomicReference<ItemCatalogReloadResult> first = new AtomicReference<>();
        Thread thread = new Thread(() -> first.set(coordinator.execute(operation)));
        thread.start();
        loading.await();

        ItemCatalogReloadResult second = coordinator.execute(operation);
        release.countDown();
        thread.join();

        assertEquals(ItemCatalogReloadStatus.ALREADY_RUNNING, second.status());
        assertEquals(ItemCatalogReloadStatus.SUCCESS, first.get().status());
    }

    private static CustomItemCatalog catalog(String name) {
        CustomItemDefinition definition = new CustomItemDefinition(
            "copper_apple", "copper-apple", "APPLE", "bigcasares:copper_apple", OptionalInt.of(1001),
            new ItemDisplayDefinition("item.bigcasares.copper_apple", name, List.of()), 64, null, null, null, null,
            new ItemAppearanceDefinition(
                "java/assets/bigcasares/items/copper_apple.json",
                "bedrock/textures/item/copper_apple.png"
            )
        );
        return new CustomItemCatalog(Map.of(definition.id(), definition));
    }

    private static final class FakeOperation implements ItemCatalogReloadOperation {

        private final CustomItemCatalog oldCatalog;
        private final CustomItemCatalog candidate;
        private Throwable failure;
        private Runnable beforeLoad;
        private int commits;

        private FakeOperation(CustomItemCatalog oldCatalog, CustomItemCatalog candidate) {
            this.oldCatalog = oldCatalog;
            this.candidate = candidate;
        }

        @Override
        public Optional<CustomItemCatalog> activeCatalog() {
            return Optional.ofNullable(oldCatalog);
        }

        @Override
        public CustomItemCatalog loadCandidate() {
            if (beforeLoad != null) {
                beforeLoad.run();
            }
            if (failure instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            return candidate;
        }

        @Override
        public ItemReconciliationResult commit(CustomItemCatalog candidate) {
            commits++;
            return new ItemReconciliationResult(0, 0, 0, 0);
        }
    }
}
