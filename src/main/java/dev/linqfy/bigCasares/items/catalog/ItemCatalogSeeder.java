package dev.linqfy.bigCasares.items.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ItemCatalogSeeder {

    private final List<String> fileNames;

    public ItemCatalogSeeder(List<String> fileNames) {
        this.fileNames = List.copyOf(Objects.requireNonNull(fileNames, "fileNames"));
        if (this.fileNames.isEmpty()) {
            throw new IllegalArgumentException("at least one item definition is required");
        }
        this.fileNames.forEach(ItemCatalogSeeder::validateFileName);
    }

    public ItemCatalogSeedResult seed(ItemCatalogResourceSource resources, Path destination) {
        Objects.requireNonNull(resources, "resources");
        Path root = Objects.requireNonNull(destination, "destination").toAbsolutePath().normalize();
        int copied = 0;
        int preserved = 0;
        try {
            Files.createDirectories(root);
            for (String fileName : fileNames) {
                Path target = root.resolve(fileName).normalize();
                if (!target.startsWith(root)) {
                    throw new IllegalArgumentException("unsafe default item definition: " + fileName);
                }
                if (Files.exists(target)) {
                    preserved++;
                    continue;
                }
                try (InputStream source = resources.open(fileName)) {
                    if (source == null) {
                        throw new IllegalStateException("missing packaged item definition: " + fileName);
                    }
                    Files.copy(source, target);
                    copied++;
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("could not seed item catalog", exception);
        }
        return new ItemCatalogSeedResult(copied, preserved);
    }

    private static void validateFileName(String value) {
        String fileName = Objects.requireNonNull(value, "fileName").replace('\\', '/');
        if (!fileName.matches("[a-z0-9_-]+\\.yml")) {
            throw new IllegalArgumentException("unsafe default item definition: " + value);
        }
    }
}
