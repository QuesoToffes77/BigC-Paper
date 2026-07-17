package dev.linqfy.bigCasares.module.runtime;

import java.util.List;

public record RuntimeCleanupReport(List<RuntimeCleanupOutcome> outcomes, boolean alreadyClosed) {

    public RuntimeCleanupReport {
        outcomes = List.copyOf(outcomes);
    }

    public static RuntimeCleanupReport empty() {
        return new RuntimeCleanupReport(List.of(), false);
    }

    public boolean hasFailures() {
        return outcomes.stream().anyMatch(outcome -> !outcome.succeeded());
    }

    public List<RuntimeCleanupOutcome> failures() {
        return outcomes.stream().filter(outcome -> !outcome.succeeded()).toList();
    }
}
