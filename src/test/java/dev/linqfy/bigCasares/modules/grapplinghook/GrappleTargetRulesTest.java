package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.modules.grapplinghook.GrappleTargetRules.Kind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrappleTargetRulesTest {

    @Test
    void validLivingEntitiesAreTargets() {
        assertTrue(GrappleTargetRules.isAllowed(Kind.LIVING, true, false, false));
        assertTrue(GrappleTargetRules.isAllowed(Kind.OTHER, true, false, false));
    }

    @Test
    void selfIsNeverATarget() {
        assertFalse(GrappleTargetRules.isAllowed(Kind.SELF, true, false, false));
        assertFalse(GrappleTargetRules.isAllowed(Kind.SELF, true, false, true));
    }

    @Test
    void playersAreDeniedByDefaultAndAllowedExplicitly() {
        assertFalse(GrappleTargetRules.isAllowed(Kind.PLAYER, true, false, false));
        assertTrue(GrappleTargetRules.isAllowed(Kind.PLAYER, true, false, true));
    }

    @Test
    void displaysAreNeverTargets() {
        assertFalse(GrappleTargetRules.isAllowed(Kind.DISPLAY, true, false, false));
        assertFalse(GrappleTargetRules.isAllowed(Kind.DISPLAY, true, false, true));
    }

    @Test
    void projectilesAreNeverTargets() {
        assertFalse(GrappleTargetRules.isAllowed(Kind.PROJECTILE, true, false, false));
        assertFalse(GrappleTargetRules.isAllowed(Kind.PROJECTILE, true, false, true));
    }

    @Test
    void markersAreNeverTargets() {
        assertFalse(GrappleTargetRules.isAllowed(Kind.MARKER, true, false, false));
    }

    @Test
    void armorStandsAreNeverTargets() {
        assertFalse(GrappleTargetRules.isAllowed(Kind.ARMOR_STAND, true, false, false));
        assertFalse(GrappleTargetRules.isAllowed(Kind.ARMOR_STAND, true, false, true));
    }

    @Test
    void deadOrInvalidEntitiesAreNeverTargets() {
        assertFalse(GrappleTargetRules.isAllowed(Kind.LIVING, true, true, false));
        assertFalse(GrappleTargetRules.isAllowed(Kind.LIVING, false, false, false));
        assertFalse(GrappleTargetRules.isAllowed(Kind.LIVING, false, true, false));
        assertFalse(GrappleTargetRules.isAllowed(Kind.OTHER, true, true, true));
    }

    @Test
    void nullKindIsRejected() {
        assertFalse(GrappleTargetRules.isAllowed(null, true, false, false));
    }
}
