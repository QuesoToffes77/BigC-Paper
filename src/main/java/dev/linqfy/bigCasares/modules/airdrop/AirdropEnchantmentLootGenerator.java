package dev.linqfy.bigCasares.modules.airdrop;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.LinkedHashSet;

final class AirdropEnchantmentLootGenerator {
    private static final Map<AirdropQuality, List<Candidate>> POOLS = createPools();

    private final boolean allowCurses;

    AirdropEnchantmentLootGenerator(boolean allowCurses) {
        this.allowCurses = allowCurses;
    }

    AirdropReward generate(AirdropQuality quality, Random random) {
        List<Candidate> allowed = POOLS.get(quality).stream()
            .filter(candidate -> allowCurses || !candidate.curse())
            .toList();
        Candidate first = allowed.get(random.nextInt(allowed.size()));
        List<AirdropStoredEnchantment> enchantments = new ArrayList<>();
        enchantments.add(first.roll(random));

        double multiChance = quality == AirdropQuality.GHISTIC ? 0.40
            : quality == AirdropQuality.LEGENDARY ? 0.25 : 0.0;
        if (random.nextDouble() < multiChance) {
            List<Candidate> compatible = allowed.stream()
                .filter(candidate -> !candidate.key().equals(first.key()))
                .filter(candidate -> compatible(first.key(), candidate.key()))
                .toList();
            if (!compatible.isEmpty()) {
                enchantments.add(compatible.get(random.nextInt(compatible.size())).roll(random));
            }
        }
        return AirdropReward.book(quality, enchantments);
    }

    static boolean areCompatible(List<AirdropStoredEnchantment> enchantments) {
        for (int first = 0; first < enchantments.size(); first++) {
            for (int second = first + 1; second < enchantments.size(); second++) {
                if (!compatible(enchantments.get(first).key(), enchantments.get(second).key())) {
                    return false;
                }
            }
        }
        return true;
    }

    static Set<String> configuredKeys() {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        POOLS.values().forEach(pool -> pool.forEach(candidate -> keys.add(candidate.key())));
        return Set.copyOf(keys);
    }

    private static boolean compatible(String first, String second) {
        if (first.equals(second)) {
            return false;
        }
        Set<Set<String>> conflicts = Set.of(
            Set.of("mending", "infinity"),
            Set.of("fortune", "silk_touch"),
            Set.of("multishot", "piercing"),
            Set.of("sharpness", "smite"),
            Set.of("sharpness", "bane_of_arthropods"),
            Set.of("smite", "bane_of_arthropods")
        );
        Set<String> pair = Set.of(first, second);
        return conflicts.stream().noneMatch(pair::equals);
    }

    private static Map<AirdropQuality, List<Candidate>> createPools() {
        EnumMap<AirdropQuality, List<Candidate>> pools = new EnumMap<>(AirdropQuality.class);
        pools.put(AirdropQuality.COMMON, List.of(
            c("protection", 1, 2, 4, AirdropEnchantmentCategory.ARMOR),
            c("sharpness", 1, 2, 5, AirdropEnchantmentCategory.WEAPONS),
            c("efficiency", 1, 2, 5, AirdropEnchantmentCategory.TOOLS),
            c("unbreaking", 1, 2, 3, AirdropEnchantmentCategory.UTILITY),
            c("power", 1, 2, 5, AirdropEnchantmentCategory.BOW),
            c("quick_charge", 1, 1, 3, AirdropEnchantmentCategory.CROSSBOW)));
        pools.put(AirdropQuality.RARE, List.of(
            c("protection", 2, 3, 4, AirdropEnchantmentCategory.ARMOR),
            c("sharpness", 2, 3, 5, AirdropEnchantmentCategory.WEAPONS),
            c("efficiency", 2, 4, 5, AirdropEnchantmentCategory.TOOLS),
            c("unbreaking", 2, 3, 3, AirdropEnchantmentCategory.UTILITY),
            c("fortune", 1, 2, 3, AirdropEnchantmentCategory.TOOLS),
            c("looting", 1, 2, 3, AirdropEnchantmentCategory.WEAPONS),
            c("power", 2, 4, 5, AirdropEnchantmentCategory.BOW),
            c("quick_charge", 1, 2, 3, AirdropEnchantmentCategory.CROSSBOW)));
        pools.put(AirdropQuality.EPIC, List.of(
            c("protection", 3, 4, 4, AirdropEnchantmentCategory.ARMOR),
            c("sharpness", 3, 4, 5, AirdropEnchantmentCategory.WEAPONS),
            c("efficiency", 4, 5, 5, AirdropEnchantmentCategory.TOOLS),
            c("unbreaking", 3, 3, 3, AirdropEnchantmentCategory.UTILITY),
            c("fortune", 2, 3, 3, AirdropEnchantmentCategory.TOOLS),
            c("looting", 2, 3, 3, AirdropEnchantmentCategory.WEAPONS),
            c("power", 4, 5, 5, AirdropEnchantmentCategory.BOW),
            c("quick_charge", 2, 3, 3, AirdropEnchantmentCategory.CROSSBOW),
            c("multishot", 1, 1, 1, AirdropEnchantmentCategory.CROSSBOW)));
        pools.put(AirdropQuality.LEGENDARY, List.of(
            c("protection", 4, 4, 4, AirdropEnchantmentCategory.ARMOR),
            c("sharpness", 5, 5, 5, AirdropEnchantmentCategory.WEAPONS),
            c("efficiency", 5, 5, 5, AirdropEnchantmentCategory.TOOLS),
            c("fortune", 3, 3, 3, AirdropEnchantmentCategory.TOOLS),
            c("looting", 3, 3, 3, AirdropEnchantmentCategory.WEAPONS),
            c("power", 5, 5, 5, AirdropEnchantmentCategory.BOW),
            c("quick_charge", 3, 3, 3, AirdropEnchantmentCategory.CROSSBOW),
            treasure("mending", 1, 1, AirdropEnchantmentCategory.UTILITY),
            treasure("infinity", 1, 1, AirdropEnchantmentCategory.BOW),
            treasure("silk_touch", 1, 1, AirdropEnchantmentCategory.TOOLS),
            curse("binding_curse"), curse("vanishing_curse")));
        pools.put(AirdropQuality.GHISTIC, List.of(
            c("protection", 4, 4, 4, AirdropEnchantmentCategory.ARMOR),
            c("sharpness", 5, 5, 5, AirdropEnchantmentCategory.WEAPONS),
            c("efficiency", 5, 5, 5, AirdropEnchantmentCategory.TOOLS),
            c("fortune", 3, 3, 3, AirdropEnchantmentCategory.TOOLS),
            c("looting", 3, 3, 3, AirdropEnchantmentCategory.WEAPONS),
            c("power", 5, 5, 5, AirdropEnchantmentCategory.BOW),
            c("unbreaking", 3, 3, 3, AirdropEnchantmentCategory.UTILITY),
            c("quick_charge", 3, 3, 3, AirdropEnchantmentCategory.CROSSBOW),
            c("piercing", 4, 4, 4, AirdropEnchantmentCategory.CROSSBOW),
            treasure("mending", 1, 1, AirdropEnchantmentCategory.UTILITY),
            treasure("infinity", 1, 1, AirdropEnchantmentCategory.BOW),
            treasure("silk_touch", 1, 1, AirdropEnchantmentCategory.TOOLS),
            treasure("frost_walker", 2, 2, AirdropEnchantmentCategory.ARMOR),
            treasure("soul_speed", 3, 3, AirdropEnchantmentCategory.ARMOR),
            treasure("swift_sneak", 3, 3, AirdropEnchantmentCategory.ARMOR),
            curse("binding_curse"), curse("vanishing_curse")));
        return Map.copyOf(pools);
    }

    private static Candidate c(
        String key,
        int minimum,
        int maximum,
        int vanillaMaximum,
        AirdropEnchantmentCategory category
    ) {
        return new Candidate(key, minimum, maximum, vanillaMaximum, false, false, category);
    }

    private static Candidate treasure(
        String key,
        int level,
        int vanillaMaximum,
        AirdropEnchantmentCategory category
    ) {
        return new Candidate(key, level, level, vanillaMaximum, true, false, category);
    }

    private static Candidate curse(String key) {
        return new Candidate(key, 1, 1, 1, false, true, AirdropEnchantmentCategory.UTILITY);
    }

    private record Candidate(
        String key,
        int minimum,
        int maximum,
        int vanillaMaximum,
        boolean treasure,
        boolean curse,
        AirdropEnchantmentCategory category
    ) {
        AirdropStoredEnchantment roll(Random random) {
            int level = minimum + random.nextInt(maximum - minimum + 1);
            return new AirdropStoredEnchantment(key, level, vanillaMaximum, treasure, curse);
        }
    }
}
