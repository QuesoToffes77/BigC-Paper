package dev.linqfy.bigCasares.modules.airdrop;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class AirdropLootGenerator {
    private final AirdropQualitySettings settings;
    private final AirdropQualityPromoter promoter;
    private final AirdropEnchantmentLootGenerator enchantments;

    public AirdropLootGenerator(AirdropQualitySettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.promoter = new AirdropQualityPromoter(settings.promotionChances());
        this.enchantments = new AirdropEnchantmentLootGenerator(settings.allowCurses());
    }

    public List<AirdropReward> generate(AirdropType type, AirdropQuality baseQuality, Random random) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(baseQuality, "baseQuality");
        Objects.requireNonNull(random, "random");
        int rolls = settings.profile(baseQuality).lootRolls();
        List<AirdropReward> rewards = new ArrayList<>(rolls);
        Set<String> uniqueIds = new HashSet<>();

        for (int roll = 0; roll < rolls; roll++) {
            AirdropQuality rewardQuality = promoter.promote(baseQuality, random);
            AirdropReward reward;
            if (type == AirdropType.ENCHANT) {
                reward = enchantments.generate(rewardQuality, random);
            } else if (type == AirdropType.RARE_ITEMS
                && random.nextDouble() < settings.profile(rewardQuality).customItemChance()) {
                reward = choose(AirdropLootTable.customItems(rewardQuality), rewardQuality, random, uniqueIds);
                if (reward == null) {
                    reward = choose(AirdropLootTable.materials(type, rewardQuality), rewardQuality, random, uniqueIds);
                }
            } else {
                reward = choose(AirdropLootTable.materials(type, rewardQuality), rewardQuality, random, uniqueIds);
            }
            if (reward == null) {
                reward = AirdropReward.material("experience_bottle", 1, rewardQuality);
            }
            rewards.add(reward);
        }
        return List.copyOf(rewards);
    }

    private static AirdropReward choose(
        List<AirdropLootDefinition> pool,
        AirdropQuality quality,
        Random random,
        Set<String> uniqueIds
    ) {
        List<AirdropLootDefinition> available = pool.stream()
            .filter(entry -> !entry.unique() || !uniqueIds.contains(entry.itemId()))
            .toList();
        if (available.isEmpty()) {
            return null;
        }
        int totalWeight = available.stream().mapToInt(AirdropLootDefinition::weight).sum();
        int selected = random.nextInt(totalWeight);
        AirdropLootDefinition definition = available.getLast();
        for (AirdropLootDefinition candidate : available) {
            selected -= candidate.weight();
            if (selected < 0) {
                definition = candidate;
                break;
            }
        }
        if (definition.unique()) {
            uniqueIds.add(definition.itemId());
        }
        return definition.roll(quality, random::nextInt);
    }
}
