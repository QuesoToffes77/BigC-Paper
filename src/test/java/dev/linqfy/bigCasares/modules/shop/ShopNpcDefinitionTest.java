package dev.linqfy.bigCasares.modules.shop;

import org.bukkit.entity.Pose;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ShopNpcDefinitionTest {

    @Test
    void validatesPlayerModelSkinAndKeepsMandatoryVillagerFallback() {
        ShopNpcDefinition definition = new ShopNpcDefinition(
            "weapons", ShopNpcType.PLAYER_MODEL, "§6§lMaestro Armero",
            ShopSkinSource.PLAYER_NAME, "Casares", Pose.STANDING,
            true, true, Map.of("main-hand", "BLAZE_ROD"),
            "none", "plains", 1, false
        );

        assertEquals(ShopNpcType.VILLAGER, definition.bedrockFallback());
        assertEquals("Casares", definition.skinValue());
    }

    @Test
    void rejectsPlayerModelWithoutSkinValue() {
        assertThrows(IllegalArgumentException.class, () -> new ShopNpcDefinition(
            "weapons", ShopNpcType.PLAYER_MODEL, "Armero",
            ShopSkinSource.PLAYER_NAME, " ", Pose.STANDING,
            true, true, Map.of(), "none",
            "plains", 1, false
        ));
    }
}
