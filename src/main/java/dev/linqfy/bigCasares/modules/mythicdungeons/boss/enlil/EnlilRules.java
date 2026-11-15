package dev.linqfy.bigCasares.modules.mythicdungeons.boss.enlil;

/** Damage and phase decisions of the Enlil fight (no server types). */
public final class EnlilRules {

    private EnlilRules() {
    }

    /** Phase 1 ends once health is at or below {@link EnlilTuning#PHASE_TWO_AT} of the maximum. */
    public static boolean entersPhaseTwo(double health, double maximum) {
        return maximum > 0.0 && health <= maximum * EnlilTuning.PHASE_TWO_AT + 1.0e-9;
    }

    /**
     * Damage of one hit that reaches the health pool. Projectiles never hurt him (the wind deflects them; tablets
     * take arrows, he does not). Outside a vulnerable window nothing gets through. In phase 1 a hit cannot take him
     * below the phase-2 threshold, so the flood always happens.
     */
    public static double admit(boolean phaseOne, boolean vulnerable, boolean projectile, double health,
                               double maximum, double damage) {
        if (!vulnerable || projectile || !Double.isFinite(damage) || damage <= 0.0) {
            return 0.0;
        }
        if (phaseOne) {
            return Math.max(0.0, Math.min(damage, health - maximum * EnlilTuning.PHASE_TWO_AT));
        }
        return damage;
    }
}
