package dev.linqfy.bigCasares.modules.endevent;

public record EndEventLocation(String world, double x, double y, double z) {

    public EndEventLocation {
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("world is required");
        }
    }
}
