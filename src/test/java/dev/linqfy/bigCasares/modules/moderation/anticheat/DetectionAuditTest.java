package dev.linqfy.bigCasares.modules.moderation.anticheat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionAuditTest {
    @Test void onlyNewChecksDefaultToAuditWhileLegacyRulesKeepScoring() {
        assertFalse(new Detection("hover-flight", "").countsForScore(false));
        assertTrue(new Detection("hover-flight", "").countsForScore(true));
        assertTrue(new Detection("movement-speed", "").countsForScore(false));
        assertTrue(new Detection("upward-movement", "").countsForScore(false));
        assertTrue(new Detection("melee-reach", "").countsForScore(false));
        assertFalse(new Detection("melee-rate", "").countsForScore(false));
        assertTrue(new Detection("melee-rate", "").countsForScore(true));
        assertFalse(new Detection("item-duplicate", "").countsForScore(true));
    }

    @Test void allDuplicateSightingsAreUnattributedAndNeverScored() {
        assertTrue(new Detection("item-duplicate", "private").requiresUnattributedAudit());
        assertTrue(new Detection("item-duplicate-shared", "shared").requiresUnattributedAudit());
        assertFalse(new Detection("movement-speed", "movement").requiresUnattributedAudit());
    }
}
