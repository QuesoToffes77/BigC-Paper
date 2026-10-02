package dev.linqfy.bigCasares.modules.jeremy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeremyPowerCalculatorTest {
    private final JeremyPowerCalculator calculator = new JeremyPowerCalculator();
    private final JeremyDamageSettings damage = JeremyDamageSettings.defaults();

    @Test
    void weakPlayerReceivesMinimumJeremyDamage() {
        assertEquals(4.0, damage.damageFor(calculator.calculate(JeremyPlayerPower.unarmored())));
    }

    @Test
    void gearedPlayerReceivesHigherJeremyDamage() {
        JeremyPlayerPower geared = new JeremyPlayerPower(20, 12, 16, 40, 8, 1, 10, 8);

        assertTrue(damage.damageFor(calculator.calculate(geared)) > damage.minimum());
    }

    @Test
    void damageNeverExceedsConfiguredMaximum() {
        JeremyPlayerPower extreme = new JeremyPlayerPower(100, 100, 100, 200, 100, 10, 100, 100);

        assertEquals(damage.maximum(), damage.damageFor(calculator.calculate(extreme)));
    }

    @Test
    void powerScoreAccountsForArmorProtectionAndMaxHealth() {
        double base = calculator.calculate(JeremyPlayerPower.unarmored());
        double armor = calculator.calculate(new JeremyPlayerPower(10, 0, 0, 20, 0, 0, 1, 0));
        double protection = calculator.calculate(new JeremyPlayerPower(10, 0, 8, 20, 0, 0, 1, 0));
        double health = calculator.calculate(new JeremyPlayerPower(10, 0, 8, 40, 0, 0, 1, 0));

        assertTrue(armor > base);
        assertTrue(protection > armor);
        assertTrue(health > protection);
    }

    @Test
    void healthScalingIsModerateAndBounded() {
        JeremyHealthSettings health = new JeremyHealthSettings(40.0, 1.25);

        assertEquals(40.0, health.scaledHealth(0.0));
        assertEquals(50.0, health.scaledHealth(10_000.0));
    }
}
