package dev.linqfy.bigCasares.modules.airdrop;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirdropLootTableTest {

    @Test
    void enchantDropContainsOnlyControlledEnchantedBooks() {
        AirdropQualitySettings settings = AirdropQualitySettings.defaults();
        List<AirdropReward> books = new AirdropLootGenerator(settings).generate(
            AirdropType.ENCHANT, AirdropQuality.LEGENDARY, new Random(71));

        assertEquals(settings.profile(AirdropQuality.LEGENDARY).lootRolls(), books.size());
        assertTrue(books.stream().allMatch(AirdropReward::isEnchantedBook));
        assertTrue(books.stream().flatMap(book -> book.enchantments().stream())
            .allMatch(enchantment -> enchantment.level() <= enchantment.maxVanillaLevel()));
    }
}
