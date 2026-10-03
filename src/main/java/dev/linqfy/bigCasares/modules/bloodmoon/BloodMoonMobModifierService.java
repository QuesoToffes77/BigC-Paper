package dev.linqfy.bigCasares.modules.bloodmoon;

final class BloodMoonMobModifierService {
    private final double healthMultiplier;
    private final double speedMultiplier;

    BloodMoonMobModifierService(double healthMultiplier, double speedMultiplier) {
        this.healthMultiplier = healthMultiplier;
        this.speedMultiplier = speedMultiplier;
    }

    boolean apply(BloodMoonMobStats mob) {
        if (mob == null || mob.player() || !mob.hostile()) {
            return false;
        }
        boolean changed = false;
        if (!mob.hasModifier(BloodMoonAttribute.HEALTH)) {
            mob.addMultiplier(BloodMoonAttribute.HEALTH, healthMultiplier);
            mob.health(mob.maxHealth());
            changed = true;
        }
        if (!mob.hasModifier(BloodMoonAttribute.SPEED)) {
            mob.addMultiplier(BloodMoonAttribute.SPEED, speedMultiplier);
            changed = true;
        }
        return changed;
    }

    int applyAll(Iterable<? extends BloodMoonMobStats> mobs) {
        if (mobs == null) {
            return 0;
        }
        int changed = 0;
        for (BloodMoonMobStats mob : mobs) {
            if (apply(mob)) {
                changed++;
            }
        }
        return changed;
    }

    boolean remove(BloodMoonMobStats mob) {
        if (mob == null) {
            return false;
        }
        boolean changed = false;
        if (mob.hasModifier(BloodMoonAttribute.HEALTH)) {
            mob.removeModifier(BloodMoonAttribute.HEALTH);
            changed = true;
        }
        if (mob.hasModifier(BloodMoonAttribute.SPEED)) {
            mob.removeModifier(BloodMoonAttribute.SPEED);
            changed = true;
        }
        if (changed && mob.health() > mob.maxHealth()) {
            mob.health(mob.maxHealth());
        }
        return changed;
    }
}

enum BloodMoonAttribute {
    HEALTH,
    SPEED
}

interface BloodMoonMobStats {
    boolean hostile();

    boolean player();

    boolean hasModifier(BloodMoonAttribute attribute);

    void addMultiplier(BloodMoonAttribute attribute, double multiplier);

    void removeModifier(BloodMoonAttribute attribute);

    double maxHealth();

    double health();

    void health(double health);
}
