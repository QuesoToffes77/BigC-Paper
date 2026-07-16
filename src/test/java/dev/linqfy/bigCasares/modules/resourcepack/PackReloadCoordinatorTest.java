package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackReloadCoordinatorTest {

    @Test
    void commitsChangedCandidateAndReturnsNoOpForEqualPublication() {
        PackReloadCoordinator coordinator = new PackReloadCoordinator();
        FakeOperation changed = new FakeOperation(true);
        FakeOperation unchanged = new FakeOperation(false);

        PackReloadResult success = coordinator.execute(changed, Runnable::run, Runnable::run).join();
        PackReloadResult noOp = coordinator.execute(unchanged, Runnable::run, Runnable::run).join();

        assertEquals(PackReloadStatus.SUCCESS, success.status());
        assertEquals(1, changed.commits.get());
        assertEquals(PackReloadStatus.NO_OP, noOp.status());
        assertEquals(0, unchanged.commits.get());
        assertFalse(coordinator.isRunning());
    }

    @Test
    void onlyOneConcurrentPreparationRuns() throws Exception {
        PackReloadCoordinator coordinator = new PackReloadCoordinator();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FakeOperation first = new FakeOperation(true);
        first.onPrepare = () -> {
            entered.countDown();
            await(release);
        };
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            var firstFuture = coordinator.execute(first, worker, Runnable::run);
            assertTrue(entered.await(5, TimeUnit.SECONDS));

            PackReloadResult competing = coordinator.execute(
                new FakeOperation(true), worker, Runnable::run).join();
            release.countDown();
            PackReloadResult completed = firstFuture.get(5, TimeUnit.SECONDS);

            assertEquals(PackReloadStatus.ALREADY_RUNNING, competing.status());
            assertEquals(PackReloadStatus.SUCCESS, completed.status());
            assertEquals(1, first.prepares.get());
        } finally {
            release.countDown();
            worker.shutdownNow();
        }
    }

    @Test
    void retirementDuringPreparationPreventsCommit() throws Exception {
        PackReloadCoordinator coordinator = new PackReloadCoordinator();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FakeOperation operation = new FakeOperation(true);
        operation.onPrepare = () -> {
            entered.countDown();
            await(release);
        };
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            var resultFuture = coordinator.execute(operation, worker, Runnable::run);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            coordinator.retire();
            release.countDown();

            PackReloadResult result = resultFuture.get(5, TimeUnit.SECONDS);

            assertEquals(PackReloadStatus.CANCELLED, result.status());
            assertEquals(0, operation.commits.get());
        } finally {
            release.countDown();
            worker.shutdownNow();
        }
    }

    @Test
    void reportsPreparationFailureWithoutCommit() {
        PackReloadCoordinator coordinator = new PackReloadCoordinator();
        FakeOperation operation = new FakeOperation(true);
        operation.prepareFailure = new IllegalArgumentException("bad json");

        PackReloadResult result = coordinator.execute(operation, Runnable::run, Runnable::run).join();

        assertEquals(PackReloadStatus.FAILED, result.status());
        assertEquals(PackReloadPhase.BUILD, result.failurePhase());
        assertEquals(0, operation.commits.get());
        assertEquals("bad json", result.failure().getMessage());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static ActivePackManifest manifest(String sha) {
        String version = sha.substring(0, 12);
        ResourcePackManifest delivery = new ResourcePackManifest(
            version, "0".repeat(64), "1".repeat(40), UUID.fromString("7e1cc35b-7a9f-4af4-b930-93f936f9cb64"));
        return new ActivePackManifest(
            version, delivery.inputSha256(), delivery.javaSha1(), sha, delivery.javaUuid(),
            "bigcasares-java-" + sha.substring(0, 16) + ".zip",
            "2".repeat(64), delivery.bedrockUuid(),
            "bigcasares-bedrock-" + "2".repeat(16) + ".mcpack",
            URI.create("https://example.test/packs/pack.zip"), Instant.EPOCH);
    }

    private static final class FakeOperation implements PackReloadOperation {
        private final AtomicInteger prepares = new AtomicInteger();
        private final AtomicInteger commits = new AtomicInteger();
        private final boolean changed;
        private Runnable onPrepare = () -> { };
        private RuntimeException prepareFailure;

        private FakeOperation(boolean changed) {
            this.changed = changed;
        }

        @Override
        public Optional<ActivePackManifest> activeManifest() {
            return Optional.of(manifest("a".repeat(64)));
        }

        @Override
        public PackPublication prepare(UUID jobId) {
            prepares.incrementAndGet();
            onPrepare.run();
            if (prepareFailure != null) {
                throw prepareFailure;
            }
            return new PackPublication(manifest(changed ? "b".repeat(64) : "a".repeat(64)), changed);
        }

        @Override
        public void commit(PackPublication publication) {
            commits.incrementAndGet();
        }
    }
}
