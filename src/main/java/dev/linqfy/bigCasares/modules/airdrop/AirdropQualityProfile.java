package dev.linqfy.bigCasares.modules.airdrop;

public record AirdropQualityProfile(
    int guardCount,
    double healthMultiplier,
    double damageMultiplier,
    int equipmentLevel,
    int lootRolls,
    double customItemChance,
    double equipmentDropChance,
    boolean announceGlobally
) {
    public AirdropQualityProfile {
        if (guardCount < 0 || guardCount > 100) {
            throw new IllegalArgumentException("guardCount must be between 0 and 100");
        }
        if (healthMultiplier < 1.0 || damageMultiplier < 1.0) {
            throw new IllegalArgumentException("guard multipliers cannot weaken defenders");
        }
        if (equipmentLevel < 1 || equipmentLevel > 5) {
            throw new IllegalArgumentException("equipmentLevel must be between 1 and 5");
        }
        if (lootRolls < 1 || lootRolls > 27) {
            throw new IllegalArgumentException("lootRolls must be between 1 and 27");
        }
        validateChance("customItemChance", customItemChance);
        validateChance("equipmentDropChance", equipmentDropChance);
    }

    private static void validateChance(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0 and 1");
        }
    }
}
