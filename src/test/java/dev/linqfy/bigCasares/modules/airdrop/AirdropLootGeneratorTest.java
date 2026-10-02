package dev.linqfy.bigCasares.modules.airdrop;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirdropLootGeneratorTest {

    private final AirdropQualitySettings settings = AirdropQualitySettings.defaults();
    private final AirdropLootGenerator generator = new AirdropLootGenerator(settings);

    @Test
    void lootRollCountScalesWithQuality() {
        for (AirdropQuality quality : AirdropQuality.values()) {
            assertEquals(settings.profile(quality).lootRolls(),
                generator.generate(AirdropType.LUXURY, quality, new Random(quality.ordinal())).size());
        }
    }

    @Test
    void lootCannotDemoteBelowAirdropQualityAndNeverExceedsGhistic() {
        for (AirdropQuality quality : AirdropQuality.values()) {
            List<AirdropReward> rewards = generator.generate(AirdropType.HE, quality, new Random(91 + quality.ordinal()));
            assertTrue(rewards.stream().allMatch(reward -> reward.quality().rank() >= quality.rank()));
            assertTrue(rewards.stream().allMatch(reward -> reward.quality().rank() <= AirdropQuality.GHISTIC.rank()));
        }
    }

    @Test
    void uniqueRareItemsAreNotDuplicatedInOneDrop() {
        List<AirdropReward> rewards = generator.generate(
            AirdropType.RARE_ITEMS, AirdropQuality.GHISTIC, new Random(4));
        List<String> customIds = rewards.stream()
            .filter(AirdropReward::isCustomItem)
            .map(AirdropReward::itemId)
            .toList();

        assertEquals(customIds.size(), Set.copyOf(customIds).size());
    }

    @Test
    void commonRareItemsCannotNormallyGiveTierSix() {
        Set<String> ids = AirdropLootTable.customItems(AirdropQuality.COMMON).stream()
            .map(AirdropLootDefinition::itemId)
            .collect(Collectors.toSet());

        assertFalse(ids.contains("grappling_hook_6"));
        assertFalse(ids.contains("glider_tier_6"));
    }

    @Test
    void ghisticRareItemsCanRollTierSix() {
        Set<String> ids = AirdropLootTable.customItems(AirdropQuality.GHISTIC).stream()
            .map(AirdropLootDefinition::itemId)
            .collect(Collectors.toSet());

        assertTrue(ids.contains("grappling_hook_6"));
        assertTrue(ids.contains("glider_tier_6"));
    }

    @Test
    void enchantmentsScaleAndStayWithinLegalLevels() {
        for (AirdropQuality quality : AirdropQuality.values()) {
            List<AirdropReward> books = generator.generate(AirdropType.ENCHANT, quality,
                new Random(551 + quality.ordinal()));
            assertTrue(books.stream().allMatch(AirdropReward::isEnchantedBook));
            assertTrue(books.stream().flatMap(book -> book.enchantments().stream())
                .allMatch(enchantment -> enchantment.level() > 0
                    && enchantment.level() <= enchantment.maxVanillaLevel()));
        }

        assertTrue(generator.generate(AirdropType.ENCHANT, AirdropQuality.COMMON, new Random(55)).stream()
            .flatMap(book -> book.enchantments().stream()).allMatch(enchantment -> enchantment.level() <= 2));
        assertTrue(generator.generate(AirdropType.ENCHANT, AirdropQuality.EPIC, new Random(56)).stream()
            .flatMap(book -> book.enchantments().stream())
            .allMatch(enchantment -> enchantment.level() >= 2 || enchantment.maxVanillaLevel() == 1));
    }

    @Test
    void cursesAreDisabledByDefaultAndTreasureStartsAtLegendary() {
        for (AirdropQuality quality : AirdropQuality.values()) {
            List<AirdropReward> rewards = generator.generate(AirdropType.ENCHANT, quality,
                new Random(9123 + quality.ordinal()));
            assertTrue(rewards.stream().flatMap(reward -> reward.enchantments().stream())
                .noneMatch(AirdropStoredEnchantment::curse));
            assertTrue(rewards.stream().allMatch(reward -> reward.enchantments().stream()
                .noneMatch(AirdropStoredEnchantment::treasure)
                || reward.quality().rank() >= AirdropQuality.LEGENDARY.rank()));
        }
    }

    @Test
    void generatedMultiEnchantBooksRespectCompatibility() {
        for (int seed = 0; seed < 250; seed++) {
            List<AirdropReward> rewards = generator.generate(
                AirdropType.ENCHANT, AirdropQuality.GHISTIC, new Random(seed));
            for (AirdropReward reward : rewards) {
                assertTrue(AirdropEnchantmentLootGenerator.areCompatible(reward.enchantments()));
            }
        }
    }

    @Test
    void controlledEnchantCatalogCoversEveryRequestedCategory() {
        Set<String> keys = AirdropEnchantmentLootGenerator.configuredKeys();

        assertTrue(keys.contains("protection"));
        assertTrue(keys.contains("sharpness"));
        assertTrue(keys.contains("efficiency"));
        assertTrue(keys.contains("power"));
        assertTrue(keys.contains("quick_charge"));
        assertTrue(keys.contains("mending"));
    }
}
