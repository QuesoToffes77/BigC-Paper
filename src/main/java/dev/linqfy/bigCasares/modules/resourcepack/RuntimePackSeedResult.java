package dev.linqfy.bigCasares.modules.resourcepack;

public record RuntimePackSeedResult(int copiedFiles, int preservedFiles) {

    public RuntimePackSeedResult {
        if (copiedFiles < 0 || preservedFiles < 0) {
            throw new IllegalArgumentException("Seed file counts must be non-negative");
        }
    }
}
