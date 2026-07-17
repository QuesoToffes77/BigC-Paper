package dev.linqfy.bigCasares.module;

import dev.linqfy.bigCasares.module.runtime.RuntimeGeneration;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleManagerLifecycleTest {

    @Test
    void reportsEnabledAndDisabledByConfigModules() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("modules.off.enabled", false);
        ModuleManager manager = manager(config);
        FakeModule enabled = new FakeModule("enabled", new ArrayList<>());
        FakeModule off = new FakeModule("off", new ArrayList<>());
        manager.register(enabled);
        manager.register(off);

        ModuleLifecycleReport report = manager.enableRegisteredModules();

        assertEquals(List.of(ModuleLifecycleStatus.ENABLED, ModuleLifecycleStatus.DISABLED_BY_CONFIG),
            report.outcomes().stream().map(ModuleLifecycleOutcome::status).toList());
        assertFalse(report.hasFailures());
        assertEquals(1, manager.getActiveModuleCount());
        assertEquals(0, off.enableCount);
    }

    @Test
    void failedCandidateEnableStopsAndRollsBackInReverseOrder() {
        List<String> events = new ArrayList<>();
        ModuleManager manager = manager(new YamlConfiguration());
        FakeModule first = new FakeModule("first", events);
        FakeModule second = new FakeModule("second", events);
        second.failEnable = true;
        FakeModule neverAttempted = new FakeModule("third", events);
        manager.register(first);
        manager.register(second);
        manager.register(neverAttempted);

        ModuleLifecycleReport report = manager.enableRegisteredModules();

        assertEquals(List.of(
            "enable:first",
            "enable:second",
            "disable:second",
            "cleanup:second",
            "disable:first",
            "cleanup:first"
        ), events);
        assertEquals(List.of(
            ModuleLifecycleStatus.ENABLED,
            ModuleLifecycleStatus.ENABLE_FAILED,
            ModuleLifecycleStatus.DISABLED,
            ModuleLifecycleStatus.DISABLED
        ), report.outcomes().stream().map(ModuleLifecycleOutcome::status).toList());
        assertTrue(report.hasFailures());
        assertEquals(0, manager.getActiveModuleCount());
        assertEquals(0, neverAttempted.enableCount);
    }

    @Test
    void reverseDisableContinuesAfterFailureAndIsIdempotent() {
        List<String> events = new ArrayList<>();
        ModuleManager manager = manager(new YamlConfiguration());
        FakeModule first = new FakeModule("first", events);
        FakeModule second = new FakeModule("second", events);
        FakeModule third = new FakeModule("third", events);
        second.failDisable = true;
        manager.register(first);
        manager.register(second);
        manager.register(third);
        manager.enableRegisteredModules();
        events.clear();

        ModuleLifecycleReport firstDisable = manager.disableActiveModules();
        ModuleLifecycleReport secondDisable = manager.disableActiveModules();

        assertEquals(List.of(
            "disable:third",
            "cleanup:third",
            "disable:second",
            "cleanup:second",
            "disable:first",
            "cleanup:first"
        ), events);
        assertEquals(List.of(
            ModuleLifecycleStatus.DISABLED,
            ModuleLifecycleStatus.DISABLE_FAILED,
            ModuleLifecycleStatus.DISABLED
        ), firstDisable.outcomes().stream().map(ModuleLifecycleOutcome::status).toList());
        assertTrue(firstDisable.hasFailures());
        assertTrue(secondDisable.outcomes().isEmpty());
        assertEquals(0, manager.getActiveModuleCount());
    }

    @Test
    void givesEnabledModulesDistinctScopesFromOneGeneration() {
        ModuleManager manager = manager(new YamlConfiguration());
        FakeModule first = new FakeModule("first", new ArrayList<>());
        FakeModule second = new FakeModule("second", new ArrayList<>());
        manager.register(first);
        manager.register(second);
        RuntimeGeneration generation = new RuntimeGeneration(19);

        manager.enableRegisteredModules(generation);

        assertNotSame(first.scope, second.scope);
        assertEquals(generation, first.scope.generation());
        assertEquals(generation, second.scope.generation());
        assertEquals("first", first.scope.ownerId());
        assertEquals("second", second.scope.ownerId());
    }

    @Test
    void reportsEveryDisableAndCleanupFailureWithoutStoppingTeardown() {
        List<String> events = new ArrayList<>();
        ModuleManager manager = manager(new YamlConfiguration());
        FakeModule first = new FakeModule("first", events);
        FakeModule second = new FakeModule("second", events);
        first.failCleanup = true;
        second.failDisable = true;
        manager.register(first);
        manager.register(second);
        manager.enableRegisteredModules(new RuntimeGeneration(3));
        events.clear();

        ModuleLifecycleReport report = manager.disableActiveModules();

        assertEquals(List.of("disable:second", "cleanup:second", "disable:first", "cleanup:first"), events);
        assertEquals(List.of(
            ModuleLifecycleStatus.DISABLE_FAILED,
            ModuleLifecycleStatus.DISABLED,
            ModuleLifecycleStatus.CLEANUP_FAILED
        ), report.outcomes().stream().map(ModuleLifecycleOutcome::status).toList());
        assertEquals(2, report.failures().size());
    }

    @Test
    void failedModuleEnableReceivesDisableAndScopeCleanupFailuresAreReported() {
        List<String> events = new ArrayList<>();
        ModuleManager manager = manager(new YamlConfiguration());
        FakeModule module = new FakeModule("broken", events);
        module.failEnable = true;
        module.failDisable = true;
        module.failCleanup = true;
        manager.register(module);

        ModuleLifecycleReport report = manager.enableRegisteredModules(new RuntimeGeneration(4));

        assertEquals(List.of("enable:broken", "disable:broken", "cleanup:broken"), events);
        assertEquals(List.of(
            ModuleLifecycleStatus.ENABLE_FAILED,
            ModuleLifecycleStatus.DISABLE_FAILED,
            ModuleLifecycleStatus.CLEANUP_FAILED
        ), report.outcomes().stream().map(ModuleLifecycleOutcome::status).toList());
        assertEquals(0, manager.getActiveModuleCount());
    }

    @Test
    void rejectsActivationWithRetiredGeneration() {
        ModuleManager manager = manager(new YamlConfiguration());
        FakeModule module = new FakeModule("retired", new ArrayList<>());
        manager.register(module);
        RuntimeGeneration generation = new RuntimeGeneration(5);
        generation.retire();

        assertThrows(IllegalStateException.class, () -> manager.enableRegisteredModules(generation));

        assertEquals(0, module.enableCount);
        assertEquals(0, manager.getActiveModuleCount());
    }

    private static ModuleManager manager(YamlConfiguration config) {
        return new ModuleManager(config, Logger.getLogger("module-lifecycle-test"));
    }

    private static final class FakeModule implements PluginModule {
        private final String id;
        private final List<String> events;
        private int enableCount;
        private boolean failEnable;
        private boolean failDisable;
        private boolean failCleanup;
        private RuntimeRegistrationScope scope;

        private FakeModule(String id, List<String> events) {
            this.id = id;
            this.events = events;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public void onEnable() {
            enableCount++;
            events.add("enable:" + id);
            if (failEnable) {
                throw new IllegalStateException("enable " + id);
            }
        }

        @Override
        public void onEnable(RuntimeRegistrationScope scope) {
            this.scope = scope;
            scope.register("cleanup", () -> {
                events.add("cleanup:" + id);
                if (failCleanup) {
                    throw new IllegalStateException("cleanup " + id);
                }
            });
            onEnable();
        }

        @Override
        public void onDisable() {
            events.add("disable:" + id);
            if (failDisable) {
                throw new IllegalStateException("disable " + id);
            }
        }
    }
}
