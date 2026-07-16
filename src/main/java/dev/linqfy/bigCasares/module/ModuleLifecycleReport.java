package dev.linqfy.bigCasares.module;

import java.util.List;

public record ModuleLifecycleReport(List<ModuleLifecycleOutcome> outcomes) {

    public ModuleLifecycleReport {
        outcomes = List.copyOf(outcomes);
    }

    public static ModuleLifecycleReport empty() {
        return new ModuleLifecycleReport(List.of());
    }

    public boolean hasFailures() {
        return outcomes.stream().anyMatch(outcome -> outcome.status().isFailure());
    }

    public List<ModuleLifecycleOutcome> failures() {
        return outcomes.stream().filter(outcome -> outcome.status().isFailure()).toList();
    }
}
