package dev.linqfy.bigCasares.modules.jeremy;

final class JeremyPowerCalculator {
    double calculate(JeremyPlayerPower power) {
        if (power == null) {
            return 0.0;
        }
        return nonNegative(power.armorPoints()) * 2.0
            + nonNegative(power.armorToughness()) * 1.5
            + Math.max(0, power.protectionLevels()) * 1.25
            + Math.max(0.0, power.maxHealth() - 20.0) * 1.5
            + nonNegative(power.absorption()) * 0.75
            + Math.max(0, power.resistanceLevel()) * 8.0
            + nonNegative(power.weaponDamage()) * 2.0
            + Math.max(0, power.offensiveEnchantmentLevels()) * 1.5;
    }

    private static double nonNegative(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
    }
}

record JeremyPlayerPower(
    double armorPoints,
    double armorToughness,
    int protectionLevels,
    double maxHealth,
    double absorption,
    int resistanceLevel,
    double weaponDamage,
    int offensiveEnchantmentLevels
) {
    static JeremyPlayerPower unarmored() {
        return new JeremyPlayerPower(0, 0, 0, 20, 0, 0, 0, 0);
    }
}
