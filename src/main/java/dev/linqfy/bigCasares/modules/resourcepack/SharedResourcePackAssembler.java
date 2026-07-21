package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

final class SharedResourcePackAssembler {

    void assemble(Path sourceRoot, Path javaRoot, Path bedrockRoot, ResourcePackRegistry registry) throws IOException {
        Path shared = sourceRoot.resolve("shared");
        for (ResourcePackAsset asset : registry.assets()) {
            copySharedTexture(shared, javaRoot, bedrockRoot, asset);
        }
        copySharedSounds(shared.resolve("sounds"), javaRoot.resolve("assets/bigcasares/sounds"));
        copySharedSounds(shared.resolve("sounds"), bedrockRoot.resolve("sounds"));
        generateJavaItemDefinitions(javaRoot, registry);
        generateBedrockItemTextures(bedrockRoot, registry);
        createEntityLayouts(javaRoot, bedrockRoot);
    }

    private void copySharedTexture(Path shared, Path javaRoot, Path bedrockRoot, ResourcePackAsset asset)
        throws IOException {
        if (asset.texture() == null || asset.usesBuiltinTexture()) return;
        Path source = shared.resolve("textures").resolve(asset.texturePath() + ".png");
        if (!Files.isRegularFile(source)) return;
        copy(source, javaRoot.resolve("assets/bigcasares/textures").resolve(asset.texturePath() + ".png"));
        copy(source, bedrockRoot.resolve("textures").resolve(asset.texturePath() + ".png"));
    }

    private void generateJavaItemDefinitions(Path javaRoot, ResourcePackRegistry registry) throws IOException {
        for (ResourcePackAsset asset : registry.assets()) {
            if (!"item-model".equals(asset.type()) || asset.javaModel() == null) continue;
            String[] model = asset.javaModel().split(":", 2);
            String itemName = model[1].startsWith("item/") ? model[1].substring(5) : model[1];
            Path definition = javaRoot.resolve("assets").resolve(model[0]).resolve("items").resolve(itemName + ".json");
            Files.createDirectories(definition.getParent());
            Files.writeString(definition, """
                {
                  "model": {"type": "minecraft:model", "model": "%s"}
                }
                """.formatted(asset.javaModel()), StandardCharsets.UTF_8);
        }
    }

    private void generateBedrockItemTextures(Path bedrockRoot, ResourcePackRegistry registry) throws IOException {
        StringBuilder entries = new StringBuilder();
        for (ResourcePackAsset asset : registry.assets()) {
            if (!"item-model".equals(asset.type()) || asset.texture() == null) continue;
            if (!entries.isEmpty()) entries.append(",\n");
            entries.append("    \"bigcasares.").append(asset.id().replace('-', '_')).append("\": {\"textures\": \"textures/")
                .append(asset.texturePath()).append("\"}");
        }
        Path output = bedrockRoot.resolve("textures/item_texture.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, """
            {
              "resource_pack_name": "bigcasares",
              "texture_name": "atlas.items",
              "texture_data": {
            %s
              }
            }
            """.formatted(entries), StandardCharsets.UTF_8);
    }

    private void createEntityLayouts(Path javaRoot, Path bedrockRoot) throws IOException {
        for (Path directory : java.util.List.of(
            javaRoot.resolve("assets/bigcasares/models/entity"),
            javaRoot.resolve("assets/bigcasares/textures/entity"),
            bedrockRoot.resolve("entity"), bedrockRoot.resolve("models/entity"),
            bedrockRoot.resolve("textures/entity"), bedrockRoot.resolve("animations"),
            bedrockRoot.resolve("animation_controllers"), bedrockRoot.resolve("render_controllers")
        )) Files.createDirectories(directory);
    }

    private void copySharedSounds(Path source, Path destination) throws IOException {
        if (!Files.isDirectory(source)) return;
        try (var files = Files.walk(source)) {
            for (Path file : files.filter(Files::isRegularFile).sorted(Comparator.naturalOrder()).toList()) {
                copy(file, destination.resolve(source.relativize(file)));
            }
        }
    }

    private void copy(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }
}
