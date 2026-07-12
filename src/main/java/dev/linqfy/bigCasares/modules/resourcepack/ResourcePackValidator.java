package dev.linqfy.bigCasares.modules.resourcepack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ResourcePackValidator {

    public void validate(ResourcePackRegistry registry, Path javaRoot, Path bedrockRoot) {
        List<Path> missing = new ArrayList<>();
        require(javaRoot.resolve("pack.mcmeta"), missing);
        require(bedrockRoot.resolve("manifest.json"), missing);

        for (ResourcePackAsset asset : registry.assets()) {
            if (asset.javaModel() != null) {
                NamespacedPath model = NamespacedPath.parse(asset.javaModel());
                require(javaRoot.resolve("assets").resolve(model.namespace()).resolve("models")
                    .resolve(model.path() + ".json"), missing);
            }
            if (asset.bedrockEntity() != null) {
                NamespacedPath entity = NamespacedPath.parse(asset.bedrockEntity());
                require(bedrockRoot.resolve("entity").resolve(entity.path() + ".entity.json"), missing);
            }
            if (asset.texture() != null) {
                require(javaRoot.resolve("assets/bigcasares/textures").resolve(asset.texture() + ".png"), missing);
                require(bedrockRoot.resolve("textures").resolve(asset.texture() + ".png"), missing);
            }
            if (asset.javaSound() != null) {
                NamespacedPath sound = NamespacedPath.parse(asset.javaSound());
                require(javaRoot.resolve("assets").resolve(sound.namespace()).resolve("sounds.json"), missing);
            }
            if (asset.bedrockSound() != null) {
                require(bedrockRoot.resolve("sounds/sound_definitions.json"), missing);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required resource-pack files: " + missing);
        }
    }

    private static void require(Path path, List<Path> missing) {
        if (!Files.isRegularFile(path)) {
            missing.add(path);
        }
    }

    private record NamespacedPath(String namespace, String path) {
        static NamespacedPath parse(String value) {
            String[] parts = value.split(":", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank() || parts[1].contains("..")) {
                throw new IllegalArgumentException("Invalid namespaced resource id: " + value);
            }
            return new NamespacedPath(parts[0], parts[1]);
        }
    }
}
