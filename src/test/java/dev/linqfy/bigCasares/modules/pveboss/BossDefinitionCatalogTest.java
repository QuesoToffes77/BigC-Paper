package dev.linqfy.bigCasares.modules.pveboss;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BossDefinitionCatalogTest {

    @Test
    void resolvesBothConfiguredBossesByNormalizedId() {
        AbyssGuardianDefinition abyss = definition("abyss-guardian");
        AbyssGuardianDefinition sahur = definition("tung-tung-sahur");
        BossDefinitionCatalog catalog = new BossDefinitionCatalog(List.of(abyss, sahur));

        assertSame(abyss, catalog.require("ABYSS-GUARDIAN"));
        assertSame(sahur, catalog.require(" tung-tung-sahur "));
        assertEquals(List.of("abyss-guardian", "tung-tung-sahur"), catalog.ids());
    }

    @Test
    void rejectsDuplicateIdsAndUnknownLookups() {
        AbyssGuardianDefinition definition = definition("same-id");

        assertThrows(IllegalArgumentException.class,
            () -> new BossDefinitionCatalog(List.of(definition, definition)));
        BossDefinitionCatalog catalog = new BossDefinitionCatalog(List.of(definition));
        assertThrows(IllegalArgumentException.class, () -> catalog.require("missing"));
    }

    private static AbyssGuardianDefinition definition(String id) {
        var stream = BossDefinitionCatalogTest.class.getClassLoader()
            .getResourceAsStream("bosses/abyss-guardian.yml");
        assertNotNull(stream, "missing abyss guardian test fixture");
        var yaml = YamlConfiguration.loadConfiguration(
            new InputStreamReader(stream, StandardCharsets.UTF_8));
        yaml.set("id", id);
        return new AbyssGuardianDefinitionLoader().load(yaml);
    }
}
