package dev.linqfy.bigCasares.modules.jeremy;

final class JeremyGoldenHelmetPolicy {
    private JeremyGoldenHelmetPolicy() {
    }

    static double damage(double baseDamage, boolean victimIsJeremy, String equippedHelmet, double multiplier) {
        if (!victimIsJeremy || !"GOLDEN_HELMET".equals(equippedHelmet)) {
            return baseDamage;
        }
        return baseDamage * Math.max(1.0, multiplier);
    }
}
