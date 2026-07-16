package dev.linqfy.bigCasares.reload;

import dev.linqfy.bigCasares.module.ModuleLifecycleOutcome;
import dev.linqfy.bigCasares.module.ModuleLifecycleReport;
import dev.linqfy.bigCasares.module.ModuleLifecycleStatus;
import dev.linqfy.bigCasares.module.runtime.RuntimeCleanupOutcome;
import dev.linqfy.bigCasares.module.runtime.RuntimeCleanupReport;
import dev.linqfy.bigCasares.module.runtime.RuntimeGeneration;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadCoordinatorTest {

    @Test
    void rejectsNestedReloadWithoutExecutingItsOperation() {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        FakeOperation outer = new FakeOperation();
        FakeOperation nested = new FakeOperation();
        AtomicReference<ReloadResult> nestedResult = new AtomicReference<>();
        outer.onDisable = () -> nestedResult.set(coordinator.execute(nested));

        ReloadResult result = coordinator.execute(outer);

        assertEquals(ReloadStatus.SUCCESS, result.status());
        assertEquals(ReloadStatus.ALREADY_RUNNING, nestedResult.get().status());
        assertEquals(0, nestedResult.get().oldGenerationId());
        assertEquals(0, nestedResult.get().newGenerationId());
        assertEquals(0, nested.disableCalls);
        assertFalse(coordinator.isReloading());
    }

    @Test
    void oldShutdownFailureCleansUpThenFailsBeforeConfigurationReload() {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        FakeOperation operation = new FakeOperation();
        operation.disableReport = report(ModuleLifecycleStatus.DISABLE_FAILED);

        ReloadResult result = coordinator.execute(operation);

        assertEquals(ReloadStatus.FAILED, result.status());
        assertEquals(ReloadPhase.SHUTDOWN, result.failurePhase());
        assertEquals(1, operation.cleanupCalls);
        assertEquals(0, operation.configurationCalls);
        assertEquals(0, operation.initializeCalls);
        assertFalse(coordinator.isReloading());
    }

    @Test
    void oldCleanupFailureFailsClosedBeforeCandidateActivation() {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        FakeOperation operation = new FakeOperation();
        operation.cleanupReport = cleanupFailure("tasks");

        ReloadResult result = coordinator.execute(operation);

        assertEquals(ReloadStatus.FAILED, result.status());
        assertEquals(ReloadPhase.CLEANUP, result.failurePhase());
        assertEquals(0, operation.configurationCalls);
        assertEquals(0, operation.initializeCalls);
    }

    @Test
    void failedCandidateIsDisabledAndCleanedBeforeReturningFailure() {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        FakeOperation operation = new FakeOperation();
        operation.initializeReport = report(ModuleLifecycleStatus.ENABLE_FAILED);

        ReloadResult result = coordinator.execute(operation);

        assertEquals(ReloadStatus.FAILED, result.status());
        assertEquals(ReloadPhase.CANDIDATE_ENABLE, result.failurePhase());
        assertEquals(2, operation.disableCalls);
        assertEquals(2, operation.cleanupCalls);
        assertEquals(0, operation.bindCalls);
        assertEquals(0, result.activeModules());
        assertFalse(operation.suppliedGeneration.isActive());
        assertFalse(coordinator.isReloading());
    }

    @Test
    void releasesGuardWhenOperationThrows() {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        FakeOperation broken = new FakeOperation();
        broken.configurationFailure = new IllegalStateException("bad yaml");

        ReloadResult failed = coordinator.execute(broken);
        ReloadResult retried = coordinator.execute(new FakeOperation());

        assertEquals(ReloadStatus.FAILED, failed.status());
        assertEquals(ReloadPhase.CONFIGURATION, failed.failurePhase());
        assertEquals(ReloadStatus.SUCCESS, retried.status());
        assertFalse(coordinator.isReloading());
    }

    @Test
    void returnsNoOpWithDurationAndUnchangedGeneration() {
        ReloadCoordinator coordinator = new ReloadCoordinator(12);
        FakeOperation operation = new FakeOperation();
        operation.noOp = true;

        ReloadResult result = coordinator.execute(operation);

        assertEquals(ReloadStatus.NO_OP, result.status());
        assertEquals(12, result.oldGenerationId());
        assertEquals(12, result.newGenerationId());
        assertEquals(0, operation.disableCalls);
        assertFalse(result.duration().isNegative());
    }

    @Test
    void suppliesCandidateGenerationThatMatchesSuccessfulResult() {
        ReloadCoordinator coordinator = new ReloadCoordinator(5);
        FakeOperation operation = new FakeOperation();

        ReloadResult result = coordinator.execute(operation);

        assertEquals(6, operation.suppliedGeneration.id());
        assertEquals(result.newGenerationId(), operation.suppliedGeneration.id());
    }

    @Test
    void failedCandidateGenerationIdsAreNeverReused() {
        ReloadCoordinator coordinator = new ReloadCoordinator(5);
        FakeOperation first = new FakeOperation();
        first.initializeReport = report(ModuleLifecycleStatus.ENABLE_FAILED);
        FakeOperation second = new FakeOperation();
        second.initializeReport = report(ModuleLifecycleStatus.ENABLE_FAILED);
        FakeOperation third = new FakeOperation();

        ReloadResult firstResult = coordinator.execute(first);
        ReloadResult secondResult = coordinator.execute(second);
        ReloadResult thirdResult = coordinator.execute(third);

        assertEquals(6, firstResult.newGenerationId());
        assertEquals(7, secondResult.newGenerationId());
        assertEquals(8, thirdResult.newGenerationId());
        assertEquals(8, coordinator.currentGenerationId());
    }

    @Test
    void allowsOnlyOneConcurrentOperationToExecute() throws Exception {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        FakeOperation first = new FakeOperation();
        first.onDisable = () -> {
            entered.countDown();
            await(release);
        };
        AtomicReference<ReloadResult> firstResult = new AtomicReference<>();
        Thread worker = new Thread(() -> firstResult.set(coordinator.execute(first)));
        worker.start();
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        FakeOperation competing = new FakeOperation();

        ReloadResult competingResult = coordinator.execute(competing);
        release.countDown();
        worker.join(5000);

        assertEquals(ReloadStatus.ALREADY_RUNNING, competingResult.status());
        assertEquals(0, competing.disableCalls);
        assertEquals(ReloadStatus.SUCCESS, firstResult.get().status());
        assertFalse(worker.isAlive());
    }

    @Test
    void tenCyclesRetireEveryOldScopeAndOnlyNewestCallbackRuns() {
        ReloadCoordinator coordinator = new ReloadCoordinator();
        CyclingOperation operation = new CyclingOperation();

        for (int cycle = 0; cycle < 10; cycle++) {
            ReloadResult result = coordinator.execute(operation);
            assertEquals(ReloadStatus.SUCCESS, result.status());
            assertEquals(1, operation.activeRegistrations.get());
            assertNotEquals(result.oldGenerationId(), result.newGenerationId());
        }
        operation.callbacks.forEach(Runnable::run);

        assertEquals(9, operation.closedScopes.get());
        assertEquals(1, operation.mutations.get());
        assertEquals(1, operation.activeRegistrations.get());
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

    private static ModuleLifecycleReport report(ModuleLifecycleStatus status) {
        return new ModuleLifecycleReport(List.of(new ModuleLifecycleOutcome(
            "test-module",
            status,
            status.isFailure() ? new IllegalStateException("module failure") : null
        )));
    }

    private static RuntimeCleanupReport cleanupFailure(String resourceId) {
        return new RuntimeCleanupReport(List.of(new RuntimeCleanupOutcome(
            resourceId,
            new IllegalStateException("cleanup failure")
        )), false);
    }

    private static final class FakeOperation implements ReloadOperation {
        private ModuleLifecycleReport disableReport = ModuleLifecycleReport.empty();
        private ModuleLifecycleReport initializeReport = report(ModuleLifecycleStatus.ENABLED);
        private RuntimeCleanupReport cleanupReport = RuntimeCleanupReport.empty();
        private RuntimeException configurationFailure;
        private RuntimeGeneration suppliedGeneration;
        private Runnable onDisable = () -> { };
        private boolean noOp;
        private int disableCalls;
        private int cleanupCalls;
        private int configurationCalls;
        private int initializeCalls;
        private int bindCalls;

        @Override
        public boolean isNoOp() {
            return noOp;
        }

        @Override
        public ModuleLifecycleReport disableRuntime() {
            disableCalls++;
            onDisable.run();
            return disableReport;
        }

        @Override
        public RuntimeCleanupReport cleanupRuntime() {
            cleanupCalls++;
            return cleanupReport;
        }

        @Override
        public void reloadConfiguration() {
            configurationCalls++;
            if (configurationFailure != null) {
                throw configurationFailure;
            }
        }

        @Override
        public ModuleLifecycleReport initializeRuntime(RuntimeGeneration generation) {
            suppliedGeneration = generation;
            initializeCalls++;
            return initializeReport;
        }

        @Override
        public void bindCommands() {
            bindCalls++;
        }

        @Override
        public int activeModuleCount() {
            return initializeReport.hasFailures() ? 0 : 1;
        }

        @Override
        public int registeredModuleCount() {
            return 1;
        }
    }

    private static final class CyclingOperation implements ReloadOperation {
        private final List<Runnable> callbacks = new ArrayList<>();
        private final AtomicInteger activeRegistrations = new AtomicInteger();
        private final AtomicInteger closedScopes = new AtomicInteger();
        private final AtomicInteger mutations = new AtomicInteger();
        private RuntimeRegistrationScope scope;

        @Override
        public ModuleLifecycleReport disableRuntime() {
            return ModuleLifecycleReport.empty();
        }

        @Override
        public RuntimeCleanupReport cleanupRuntime() {
            return scope == null ? RuntimeCleanupReport.empty() : scope.close();
        }

        @Override
        public void reloadConfiguration() {
        }

        @Override
        public ModuleLifecycleReport initializeRuntime(RuntimeGeneration generation) {
            scope = new RuntimeRegistrationScope(generation, "cycle");
            activeRegistrations.incrementAndGet();
            scope.register("registration", () -> {
                activeRegistrations.decrementAndGet();
                closedScopes.incrementAndGet();
            });
            callbacks.add(scope.guard(mutations::incrementAndGet));
            return report(ModuleLifecycleStatus.ENABLED);
        }

        @Override
        public void bindCommands() {
        }

        @Override
        public int activeModuleCount() {
            return 1;
        }

        @Override
        public int registeredModuleCount() {
            return 1;
        }
    }
}
