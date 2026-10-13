package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosHelmetRuleTest {

    @Test
    void cursedHelmetStaysOnInSurvival() {
        assertFalse(DonPollosHelmetRule.canReplaceHelmet(true, false));
    }

    @Test
    void creativeCanReplaceACursedHelmet() {
        assertTrue(DonPollosHelmetRule.canReplaceHelmet(true, true));
    }

    @Test
    void normalHelmetIsSwappedForTheBucket() {
        assertTrue(DonPollosHelmetRule.canReplaceHelmet(false, false));
        assertTrue(DonPollosHelmetRule.canReplaceHelmet(false, true));
    }
}
