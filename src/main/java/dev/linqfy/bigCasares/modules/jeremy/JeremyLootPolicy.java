package dev.linqfy.bigCasares.modules.jeremy;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

final class JeremyLootPolicy {

    private final JeremyLootSettings settings;
    private final Random random;

    JeremyLootPolicy(JeremyLootSettings settings) {
        this(settings, new Random());
    }

    JeremyLootPolicy(JeremyLootSettings settings, Random random) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.random = Objects.requireNonNull(random, "random");
    }

    Optional<JeremyLootRoll> roll(boolean killedByPlayer) {
        if (!killedByPlayer || !settings.enabled()) {
            return Optional.empty();
        }
        int diamonds = random.nextDouble() < settings.diamondChance() ? settings.diamondAmount() : 0;
        return Optional.of(new JeremyLootRoll(
            randomBetween(settings.minimumEmeralds(), settings.maximumEmeralds()),
            randomBetween(settings.minimumGoldIngots(), settings.maximumGoldIngots()),
            diamonds,
            settings.experience()
        ));
    }

    private int randomBetween(int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }
}

record JeremyLootSettings(
    boolean enabled,
    int experience,
    int minimumEmeralds,
    int maximumEmeralds,
    int minimumGoldIngots,
    int maximumGoldIngots,
    double diamondChance,
    int diamondAmount
) {
    JeremyLootSettings {
        experience = Math.max(0, experience);
        minimumEmeralds = Math.max(0, Math.min(64, minimumEmeralds));
        maximumEmeralds = Math.max(minimumEmeralds, Math.min(64, maximumEmeralds));
        minimumGoldIngots = Math.max(0, Math.min(64, minimumGoldIngots));
        maximumGoldIngots = Math.max(minimumGoldIngots, Math.min(64, maximumGoldIngots));
        diamondChance = Double.isFinite(diamondChance) ? Math.max(0.0, Math.min(1.0, diamondChance)) : 0.0;
        diamondAmount = Math.max(0, Math.min(8, diamondAmount));
    }

    static JeremyLootSettings defaults() {
        return new JeremyLootSettings(true, 75, 4, 8, 4, 12, 0.08, 1);
    }

    static JeremyLootSettings disabled() {
        return new JeremyLootSettings(false, 0, 0, 0, 0, 0, 0.0, 0);
    }
}

record JeremyLootRoll(int emeralds, int goldIngots, int diamonds, int experience) {
}
