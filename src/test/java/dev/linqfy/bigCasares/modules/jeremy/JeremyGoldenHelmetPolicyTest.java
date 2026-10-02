package dev.linqfy.bigCasares.modules.jeremy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JeremyGoldenHelmetPolicyTest {

    @Test
    void goldHelmetIncreasesDamageAgainstJeremy() {
        assertEquals(15.0, JeremyGoldenHelmetPolicy.damage(10.0, true, "GOLDEN_HELMET", 1.5));
    }

    @Test
    void otherEquipmentAndOtherVictimsReceiveNormalDamage() {
        assertEquals(10.0, JeremyGoldenHelmetPolicy.damage(10.0, true, null, 1.5));
        assertEquals(10.0, JeremyGoldenHelmetPolicy.damage(10.0, true, "DIAMOND_HELMET", 1.5));
        assertEquals(10.0, JeremyGoldenHelmetPolicy.damage(10.0, true, "GOLDEN_BOOTS", 1.5));
        assertEquals(10.0, JeremyGoldenHelmetPolicy.damage(10.0, false, "GOLDEN_HELMET", 1.5));
    }
}
