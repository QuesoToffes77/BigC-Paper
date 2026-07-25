package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndEventPotionPolicyTest {

    @Test
    void bansEveryConfiguredLateGamePotionItem() {
        assertTrue(EndEventPotionPolicy.isForbidden(Material.POTION));
        assertTrue(EndEventPotionPolicy.isForbidden(Material.SPLASH_POTION));
        assertTrue(EndEventPotionPolicy.isForbidden(Material.LINGERING_POTION));
        assertTrue(EndEventPotionPolicy.isForbidden(Material.TIPPED_ARROW));
        assertFalse(EndEventPotionPolicy.isForbidden(Material.GLASS_BOTTLE));
    }
}
