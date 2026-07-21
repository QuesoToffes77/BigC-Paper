package dev.linqfy.bigCasares.modules.missions;

public record MissionProgressSnapshot(int progress, boolean completed, boolean claimed, java.util.Set<String> markers) {

    public MissionProgressSnapshot(int progress, boolean completed, boolean claimed) {
        this(progress, completed, claimed, java.util.Set.of());
    }

    public MissionProgressSnapshot {
        markers = java.util.Set.copyOf(markers);
    }

    public static MissionProgressSnapshot fresh() {
        return new MissionProgressSnapshot(0, false, false, java.util.Set.of());
    }
}
