package dev.linqfy.bigCasares.modules.geyser;

import dev.linqfy.bigCasares.modules.celular.CelularService;
import dev.linqfy.bigCasares.modules.celular.CelularSettings;
import dev.linqfy.bigCasares.modules.celular.CelularVideo;
import dev.linqfy.bigCasares.modules.celular.CelularVideosLoader;
import dev.linqfy.bigCasares.modules.resourcepack.ResourcePackAsset;
import dev.linqfy.bigCasares.modules.resourcepack.ResourcePackRegistry;
import dev.linqfy.bigCasares.testsupport.CompiledClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeyserCelularItemsTest {

    @Test
    void everyJavaModelOfThePhoneBecomesABedrockItemOnTopOfAClock() {
        List<GeyserCustomItemDefinition> definitions =
            GeyserCelularItems.definitions(List.of("celular:celular", "celular:video_1", "celular:video_2"));

        assertEquals(3, definitions.size());
        for (GeyserCustomItemDefinition definition : definitions) {
            assertEquals("minecraft:clock", definition.javaBaseIdentifier());
            assertEquals(definition.javaModelIdentifier(), definition.bedrockIdentifier(),
                "the attachable of each Bedrock item uses the same identifier as the Java model");
            assertFalse(definition.bedrockIdentifier().startsWith("minecraft:"), "Geyser rejects minecraft: ids");
            assertEquals("bigcasares.celular", definition.icon());
            assertEquals("Celular", definition.displayName());
        }
        assertEquals(List.of("celular:celular", "celular:video_1", "celular:video_2"),
            definitions.stream().map(GeyserCustomItemDefinition::javaModelIdentifier).toList());
    }

    @Test
    void packagedPhoneGetsOneItemPerVideoPlusTheStillPhone() throws IOException {
        List<CelularVideo> videos;
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(CelularVideosLoader.RESOURCE)) {
            videos = CelularVideosLoader.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        List<GeyserCustomItemDefinition> definitions = GeyserCelularItems.definitions(
            new CelularService(videos, CelularSettings.defaults()).itemModels());

        assertEquals(videos.size() + 1, definitions.size());
        assertEquals("celular:celular", definitions.get(0).javaModelIdentifier());
        for (int i = 0; i < videos.size(); i++) {
            assertEquals("celular:" + videos.get(i).id(), definitions.get(i + 1).javaModelIdentifier());
        }
    }

    @Test
    void theIconKeyComesFromTheRegistryEntryThatGeneratesItemTextureJson() throws IOException {
        ResourcePackRegistry registry = ResourcePackRegistry.load(Path.of("resourcepack/shared/registry.yml"));
        ResourcePackAsset icon = registry.assets().stream()
            .filter(asset -> asset.id().equals("celular")).findFirst().orElseThrow();
        assertEquals("item-model", icon.type());
        assertEquals("item/celular_bedrock", icon.texturePath());
        assertEquals("bigcasares." + icon.id().replace('-', '_'),
            GeyserCelularItems.definitions(List.of("celular:celular")).get(0).icon());
    }

    @Test
    void geyserModuleRegistersThePhoneItemsThroughTheFactory() throws IOException {
        CompiledClass module = CompiledClass.of(getClass(), GeyserIntegrationModule.class.getName());
        assertTrue(module.calls("dev/linqfy/bigCasares/modules/geyser/GeyserCelularItems", "definitions"));
        assertTrue(module.calls("dev/linqfy/bigCasares/modules/celular/CelularModule", "itemModels"));
        assertTrue(module.calls("dev/linqfy/bigCasares/BigCasares", "getCelularModule"));
    }
}
