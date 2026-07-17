package dev.linqfy.bigCasares.reload;

import dev.linqfy.bigCasares.module.ModuleLifecycleOutcome;
import dev.linqfy.bigCasares.module.runtime.RuntimeCleanupOutcome;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReloadResultTest {

    @Test
    void copiesEveryCollectionExposedByResult() {
        List<ModuleLifecycleOutcome> modules = new ArrayList<>();
        List<RuntimeCleanupOutcome> cleanups = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        ReloadResult result = ReloadResult.success(
            Duration.ofMillis(3),
            4,
            5,
            modules,
            cleanups,
            warnings,
            1,
            1
        );

        modules.add(null);
        cleanups.add(null);
        warnings.add("late warning");

        assertTrue(result.moduleOutcomes().isEmpty());
        assertTrue(result.cleanupOutcomes().isEmpty());
        assertTrue(result.warnings().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.warnings().add("mutation"));
    }

    @Test
    void rejectsNegativeDurationAndImpossibleModuleCounts() {
        assertThrows(IllegalArgumentException.class, () -> ReloadResult.success(
            Duration.ofNanos(-1), 1, 2, List.of(), List.of(), List.of(), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ReloadResult.success(
            Duration.ZERO, 1, 2, List.of(), List.of(), List.of(), 2, 1));
    }
}
