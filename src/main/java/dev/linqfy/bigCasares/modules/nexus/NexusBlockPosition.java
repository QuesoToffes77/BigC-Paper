package dev.linqfy.bigCasares.modules.nexus;

public record NexusBlockPosition(int x, int y, int z) {

    public NexusBlockPosition offset(int deltaX, int deltaY, int deltaZ) {
        return new NexusBlockPosition(x + deltaX, y + deltaY, z + deltaZ);
    }
}
