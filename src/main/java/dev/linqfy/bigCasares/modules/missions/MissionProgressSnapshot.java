package dev.linqfy.bigCasares.modules.missions;

public record MissionProgressSnapshot(int progress, boolean completed, boolean claimed) {

    public static MissionProgressSnapshot fresh() {
        return new MissionProgressSnapshot(0, false, false);
    }
}
