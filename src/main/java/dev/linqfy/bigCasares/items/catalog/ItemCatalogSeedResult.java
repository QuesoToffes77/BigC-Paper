package dev.linqfy.bigCasares.items.catalog;

public record ItemCatalogSeedResult(int copiedFiles, int preservedFiles) {

    public ItemCatalogSeedResult {
        if (copiedFiles < 0 || preservedFiles < 0) {
            throw new IllegalArgumentException("seed counts must not be negative");
        }
    }
}
