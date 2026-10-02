package dev.linqfy.bigCasares.modules.bloodmoon;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloodMoonMobModifierServiceTest {

    @Test
    void bloodMoonAppliesOnePointFiveHealthMultiplier() {
        FakeMob mob = FakeMob.hostile(20.0, 0.23);
        service().apply(mob);

        assertEquals(30.0, mob.maxHealth(), 0.000_001);
        assertEquals(30.0, mob.health(), 0.000_001);
    }

    @Test
    void bloodMoonHealthModifierIsNotAppliedTwice() {
        FakeMob mob = FakeMob.hostile(20.0, 0.23);
        BloodMoonMobModifierService service = service();

        service.apply(mob);
        service.apply(mob);

        assertEquals(30.0, mob.maxHealth(), 0.000_001);
        assertEquals(1, mob.additions(BloodMoonAttribute.HEALTH));
    }

    @Test
    void bloodMoonAppliesConfiguredSpeedMultiplierWithoutStacking() {
        FakeMob mob = FakeMob.hostile(20.0, 0.23);
        BloodMoonMobModifierService service = service();

        service.apply(mob);
        service.apply(mob);

        assertEquals(0.2875, mob.movementSpeed(), 0.000_001);
        assertEquals(1, mob.additions(BloodMoonAttribute.SPEED));
    }

    @Test
    void endingBloodMoonRemovesOnlyBloodMoonModifiers() {
        FakeMob mob = FakeMob.hostile(20.0, 0.23);
        mob.externalHealthMultiplier = 1.2;
        BloodMoonMobModifierService service = service();
        service.apply(mob);
        mob.health = 36.0;

        service.remove(mob);

        assertFalse(mob.hasModifier(BloodMoonAttribute.HEALTH));
        assertFalse(mob.hasModifier(BloodMoonAttribute.SPEED));
        assertEquals(24.0, mob.maxHealth(), 0.000_001);
        assertEquals(24.0, mob.health(), 0.000_001);
        assertEquals(1.2, mob.externalHealthMultiplier, 0.000_001);
    }

    @Test
    void passiveMobAndPlayerAreNeverBuffed() {
        FakeMob passive = new FakeMob(false, false, 20.0, 0.23);
        FakeMob player = new FakeMob(true, true, 20.0, 0.1);

        assertFalse(service().apply(passive));
        assertFalse(service().apply(player));
        assertTrue(passive.modifiers.isEmpty());
        assertTrue(player.modifiers.isEmpty());
    }

    @Test
    void existingHostilesAreBuffedWhenBloodMoonStarts() {
        FakeMob zombie = FakeMob.hostile(20.0, 0.23);
        FakeMob skeleton = FakeMob.hostile(20.0, 0.25);
        FakeMob cow = new FakeMob(false, false, 10.0, 0.2);

        int changed = service().applyAll(List.of(zombie, skeleton, cow));

        assertEquals(2, changed);
        assertEquals(30.0, zombie.maxHealth(), 0.000_001);
        assertEquals(30.0, skeleton.maxHealth(), 0.000_001);
        assertEquals(10.0, cow.maxHealth(), 0.000_001);
    }

    @Test
    void hostileSpawnedDuringBloodMoonReceivesModifiers() {
        FakeMob newlySpawned = FakeMob.hostile(20.0, 0.23);

        assertTrue(service().apply(newlySpawned));
        assertEquals(30.0, newlySpawned.maxHealth(), 0.000_001);
        assertEquals(0.2875, newlySpawned.movementSpeed(), 0.000_001);
    }

    private static BloodMoonMobModifierService service() {
        return new BloodMoonMobModifierService(1.5, 1.25);
    }

    private static final class FakeMob implements BloodMoonMobStats {
        private final boolean hostile;
        private final boolean player;
        private final double baseHealth;
        private final double baseSpeed;
        private final Map<BloodMoonAttribute, Double> modifiers = new EnumMap<>(BloodMoonAttribute.class);
        private final Map<BloodMoonAttribute, Integer> additions = new EnumMap<>(BloodMoonAttribute.class);
        private double externalHealthMultiplier = 1.0;
        private double health;

        private FakeMob(boolean hostile, boolean player, double baseHealth, double baseSpeed) {
            this.hostile = hostile;
            this.player = player;
            this.baseHealth = baseHealth;
            this.baseSpeed = baseSpeed;
            this.health = baseHealth;
        }

        static FakeMob hostile(double health, double speed) {
            return new FakeMob(true, false, health, speed);
        }

        @Override
        public boolean hostile() {
            return hostile;
        }

        @Override
        public boolean player() {
            return player;
        }

        @Override
        public boolean hasModifier(BloodMoonAttribute attribute) {
            return modifiers.containsKey(attribute);
        }

        @Override
        public void addMultiplier(BloodMoonAttribute attribute, double multiplier) {
            modifiers.put(attribute, multiplier);
            additions.merge(attribute, 1, Integer::sum);
        }

        @Override
        public void removeModifier(BloodMoonAttribute attribute) {
            modifiers.remove(attribute);
        }

        @Override
        public double maxHealth() {
            return baseHealth * externalHealthMultiplier * modifiers.getOrDefault(BloodMoonAttribute.HEALTH, 1.0);
        }

        @Override
        public double health() {
            return health;
        }

        @Override
        public void health(double health) {
            this.health = health;
        }

        double movementSpeed() {
            return baseSpeed * modifiers.getOrDefault(BloodMoonAttribute.SPEED, 1.0);
        }

        int additions(BloodMoonAttribute attribute) {
            return additions.getOrDefault(attribute, 0);
        }
    }
}
