package dev.linqfy.bigCasares.modules.endevent;

public record EndEventBorderSnapshot(String world, double centerX, double centerZ, double size) {

    public EndEventBorderSnapshot {
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("world is required");
        }
        if (size <= 0.0) {
            throw new IllegalArgumentException("size must be positive");
        }
    }
}
