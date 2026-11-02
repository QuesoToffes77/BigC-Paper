package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosRewardTest {

    @Test
    void donPolloSaysTheFivePhrasesInOrder() {
        assertEquals(List.of(
            "Ya no hay video mi gente...",
            "Ni salsa...",
            "Ni picante...",
            "Mi gente triste...",
            "Elegi lo que quieras..."), DonPollosReward.DEFEAT_PHRASES);
    }

    @Test
    void gorditoSaysHisSecretPhrasesInOrder() {
        assertEquals(List.of("Yo se cosas...", "Queres saberlas...", "Dame Salsa, Picante, pollo frito y diamantes"),
            DonPollosReward.SECRET_PHRASES);
    }

    @Test
    void secretBlockTellsTheStoryBeforeTurningEvil() {
        assertEquals(6, DonPollosReward.BLOCK_STORY.size());
        assertEquals("Era una vez...", DonPollosReward.BLOCK_STORY.getFirst());
        assertEquals("\"No hay salsa, ni picante, ni nada mi gente\" dijo", DonPollosReward.BLOCK_STORY.get(4));
        assertEquals("Desde ese dia algo cambio en Don Pollo...", DonPollosReward.BLOCK_STORY.getLast());
    }

    @Test
    void eachDonPolloSaysGoodbyeExceptTheCommonOne() {
        assertEquals("Lastima que alguien no tiene salsa y picante...", DonPollosReward.FAREWELLS.get(DonPolloVariant.AURA_67));
        assertEquals("Como estan los precios...", DonPollosReward.FAREWELLS.get(DonPolloVariant.FINO));
        assertEquals("A alguien le falta moverse...", DonPollosReward.FAREWELLS.get(DonPolloVariant.SALSERO));
        assertEquals("Tene cuidado...", DonPollosReward.FAREWELLS.get(DonPolloVariant.GORDITO));
        assertTrue(!DonPollosReward.FAREWELLS.containsKey(DonPolloVariant.COMUN));
        assertEquals("En esta ciudad pasaron cosas...", DonPollosReward.CITY_WELCOME);
    }

    @Test
    void rewardIsTheBucketPlusFourChoices() {
        assertEquals(4, DonPollosReward.CHOICES);
    }

    @Test
    void itemsArePagedFortyFiveAtATime() {
        List<Integer> items = IntStream.range(0, 100).boxed().toList();
        assertEquals(3, DonPollosReward.pageCount(items.size(), DonPollosReward.ITEMS_PER_PAGE));
        assertEquals(1, DonPollosReward.pageCount(0, DonPollosReward.ITEMS_PER_PAGE));
        assertEquals(45, DonPollosReward.page(items, 0, 45).size());
        assertEquals(10, DonPollosReward.page(items, 2, 45).size());
        assertEquals(90, DonPollosReward.page(items, 2, 45).getFirst());
        assertTrue(DonPollosReward.page(items, 3, 45).isEmpty());
    }

    @Test
    void itemsGoToTheirCategory() {
        assertEquals(DonPollosReward.Category.COMBATE, DonPollosReward.category("DIAMOND_SWORD", false, false, false, false));
        assertEquals(DonPollosReward.Category.COMBATE, DonPollosReward.category("NETHERITE_CHESTPLATE", false, false, false, false));
        assertEquals(DonPollosReward.Category.CONSTRUCCION, DonPollosReward.category("STONE", true, true, true, false));
        assertEquals(DonPollosReward.Category.CONSTRUCCION, DonPollosReward.category("OAK_STAIRS", true, true, false, false));
        assertEquals(DonPollosReward.Category.DECORACION, DonPollosReward.category("POPPY", true, false, false, false));
        assertEquals(DonPollosReward.Category.COMIDA, DonPollosReward.category("APPLE", false, false, false, true));
        assertEquals(DonPollosReward.Category.POCIONES, DonPollosReward.category("SPLASH_POTION", false, false, false, false));
        assertEquals(DonPollosReward.Category.TRANSPORTE, DonPollosReward.category("OAK_BOAT", false, false, false, false));
        assertEquals(DonPollosReward.Category.TRANSPORTE, DonPollosReward.category("POWERED_RAIL", true, false, false, false));
        assertEquals(DonPollosReward.Category.REDSTONE, DonPollosReward.category("REPEATER", true, false, false, false));
        assertEquals(DonPollosReward.Category.HERRAMIENTAS, DonPollosReward.category("DIAMOND_PICKAXE", false, false, false, false));
        assertEquals(DonPollosReward.Category.HERRAMIENTAS, DonPollosReward.category("WATER_BUCKET", false, false, false, false));
        assertEquals(DonPollosReward.Category.VARIOS, DonPollosReward.category("STICK", false, false, false, false));
        assertEquals(9, DonPollosReward.Category.values().length);
    }

    @Test
    void itemNamesAreReadable() {
        assertEquals("Diamond Sword", DonPollosReward.pretty(Material.DIAMOND_SWORD.name()));
    }

    @Test
    void bucketHelmetGivesDepthStriderTwo() {
        assertEquals(2.0 / 3.0, DonPollosKfcBucket.DEPTH_STRIDER_II, 1e-9);
    }

    @Test
    void onlyTheWinnerCanClaimEvenWhenOffline() {
        java.util.UUID winner = java.util.UUID.randomUUID();
        java.util.UUID other = java.util.UUID.randomUUID();
        // el ganador no tiene que estar conectado: se compara su UUID guardado
        org.junit.jupiter.api.Assertions.assertTrue(DonPollosReward.canClaim(winner, winner));
        org.junit.jupiter.api.Assertions.assertFalse(DonPollosReward.canClaim(winner, other));
        // sin ganador (nadie cerca al vencerlo) lo puede cobrar cualquiera
        org.junit.jupiter.api.Assertions.assertTrue(DonPollosReward.canClaim(null, other));
    }

    @Test
    void othersAreToldTheRewardIsNotTheirs() {
        org.junit.jupiter.api.Assertions.assertEquals("§cVos no sos §fJuancraftender§c. No hay nada para vos.",
            DonPollosReward.notYoursMessage("Juancraftender"));
        org.junit.jupiter.api.Assertions.assertEquals("§cVos no sos §fel que le gano§c. No hay nada para vos.",
            DonPollosReward.notYoursMessage(null));
    }

    @Test
    void theBossNeverGivesEndDiamondOrNetheriteItems() {
        for (String banned : new String[]{"END_STONE", "END_STONE_BRICKS", "PURPUR_BLOCK", "PURPUR_STAIRS",
            "CHORUS_FRUIT", "POPPED_CHORUS_FRUIT", "SHULKER_SHELL", "SHULKER_BOX", "RED_SHULKER_BOX", "ELYTRA",
            "DRAGON_HEAD", "DRAGON_EGG", "END_ROD", "END_CRYSTAL", "DIAMOND", "DIAMOND_BLOCK", "DIAMOND_ORE",
            "DEEPSLATE_DIAMOND_ORE", "DIAMOND_SWORD", "DIAMOND_CHESTPLATE", "DIAMOND_HORSE_ARMOR", "NETHERITE_INGOT",
            "NETHERITE_BLOCK", "NETHERITE_SCRAP", "NETHERITE_PICKAXE", "NETHERITE_BOOTS", "ANCIENT_DEBRIS"}) {
            org.junit.jupiter.api.Assertions.assertTrue(DonPollosReward.bannedReward(banned), banned);
        }
    }

    @Test
    void templatesDiscsAndEverydayItemsAreAllowed() {
        for (String allowed : new String[]{"NETHERITE_UPGRADE_SMITHING_TEMPLATE", "SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE",
            "MUSIC_DISC_CAT", "JUKEBOX", "IRON_SWORD", "GOLDEN_APPLE", "ENDER_PEARL", "OAK_LOG"}) {
            org.junit.jupiter.api.Assertions.assertFalse(DonPollosReward.bannedReward(allowed), allowed);
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, DonPollosReward.AMOUNT_PER_CHOICE);
    }

    @Test
    void atMostOneTemplateAndOneDisc() {
        String[] chosen = {"MUSIC_DISC_CAT", "COAST_ARMOR_TRIM_SMITHING_TEMPLATE", null, null};
        org.junit.jupiter.api.Assertions.assertFalse(DonPollosReward.canChoose(chosen, 2, "MUSIC_DISC_13"));
        org.junit.jupiter.api.Assertions.assertFalse(DonPollosReward.canChoose(chosen, 3,
            "NETHERITE_UPGRADE_SMITHING_TEMPLATE"));
        // cambiar el disco que ya elegiste por otro si se puede
        org.junit.jupiter.api.Assertions.assertTrue(DonPollosReward.canChoose(chosen, 0, "MUSIC_DISC_13"));
        org.junit.jupiter.api.Assertions.assertTrue(DonPollosReward.canChoose(chosen, 2, "JUKEBOX"));
        org.junit.jupiter.api.Assertions.assertTrue(DonPollosReward.canChoose(new String[4], 0, "MUSIC_DISC_CAT"));
    }
}
