package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DonPollosItemTest {

    @Test
    void normalizesMaterialToUpperCase() {
        assertEquals("BREAD", new DonPollosItem(" bread ", 3).material());
    }

    @Test
    void rejectsBlankMaterial() {
        assertThrows(IllegalArgumentException.class, () -> new DonPollosItem(" ", 1));
    }

    @Test
    void rejectsAmountsOutsideOneStack() {
        assertThrows(IllegalArgumentException.class, () -> new DonPollosItem("BREAD", 0));
        assertThrows(IllegalArgumentException.class, () -> new DonPollosItem("BREAD", 641));
    }

    @Test
    void tradeRequiresCostAndResult() {
        assertThrows(NullPointerException.class, () -> new DonPollosTrade(null, new DonPollosItem("BREAD", 1)));
        assertThrows(NullPointerException.class, () -> new DonPollosTrade(new DonPollosItem("BREAD", 1), null));
    }

    @Test
    void everyProductHasAVanillaMaterialAndABedrockMapping() throws java.io.IOException {
        com.google.gson.JsonObject items;
        try (java.io.InputStream stream = getClass().getClassLoader()
            .getResourceAsStream("donpollos/bedrock/donpollos-geyser.json")) {
            org.junit.jupiter.api.Assertions.assertNotNull(stream);
            items = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,
                java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("items");
        }
        for (DonPollosProduct product : DonPollosProduct.values()) {
            String material = product.vanillaMaterial();
            org.junit.jupiter.api.Assertions.assertNotNull(org.bukkit.Material.getMaterial(material), material);
            com.google.gson.JsonArray mapped = items.getAsJsonArray("minecraft:" + material.toLowerCase(java.util.Locale.ROOT));
            org.junit.jupiter.api.Assertions.assertNotNull(mapped, product + " no tiene mapeo de Geyser");
            boolean found = false;
            for (com.google.gson.JsonElement entry : mapped) {
                found |= ("donpollos:" + product.modelKey()).equals(entry.getAsJsonObject().get("model").getAsString());
            }
            org.junit.jupiter.api.Assertions.assertTrue(found, product + " no esta en donpollos-geyser.json bajo " + material);
        }
    }
}
