package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

record ResourcePackFixture(
    Path sourceRoot,
    Path registry,
    Path javaRoot,
    Path bedrockRoot,
    Path outputRoot
) {
    static ResourcePackFixture create(Path tempDir, boolean includeTexture) throws IOException {
        Path source = tempDir.resolve("resourcepack");
        Path shared = source.resolve("shared");
        Path javaRoot = source.resolve("java");
        Path bedrockRoot = source.resolve("bedrock");
        Path output = tempDir.resolve("output");
        Files.createDirectories(shared);
        Files.createDirectories(javaRoot.resolve("assets/bigcasares/models"));
        Files.createDirectories(javaRoot.resolve("assets/bigcasares/textures/nexus"));
        Files.createDirectories(bedrockRoot.resolve("entity"));
        Files.createDirectories(bedrockRoot.resolve("textures/nexus"));

        Path registry = shared.resolve("registry.yml");
        Files.writeString(registry, """
            assets:
              nexus:
                type: entity-model
                java-model: bigcasares:nexus
                bedrock-entity: bigcasares:nexus
                texture: nexus/nexus
            """);
        Files.writeString(javaRoot.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":88,\"description\":\"test\"}}");
        Files.writeString(javaRoot.resolve("assets/bigcasares/models/nexus.json"), "{\"parent\":\"minecraft:item/generated\"}");
        Files.writeString(bedrockRoot.resolve("manifest.json"), """
            {
              "format_version": 2,
              "header": {
                "name": "BigCasares",
                "description": "test",
                "uuid": "7e1cc35b-7a9f-4af4-b930-93f936f9cb64",
                "version": [1, 0, 0],
                "min_engine_version": [1, 21, 0]
              },
              "modules": [{
                "type": "resources",
                "uuid": "fd66f86d-d853-4a7f-a01f-ad474e790f37",
                "version": [1, 0, 0]
              }]
            }
            """);
        Files.writeString(bedrockRoot.resolve("entity/nexus.entity.json"), "{\"format_version\":\"1.10.0\"}");
        if (includeTexture) {
            BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
            ImageIO.write(image, "png", javaRoot.resolve("assets/bigcasares/textures/nexus/nexus.png").toFile());
            ImageIO.write(image, "png", bedrockRoot.resolve("textures/nexus/nexus.png").toFile());
        }
        return new ResourcePackFixture(source, registry, javaRoot, bedrockRoot, output);
    }
}
