package dev.linqfy.bigCasares.modules.geyser;

public record GeyserCapabilities(
    boolean available,
    boolean resourcePacks,
    boolean customItems,
    boolean forms,
    boolean customEntities
) {
    public static GeyserCapabilities unavailable() {
        return new GeyserCapabilities(false, false, false, false, false);
    }
}
