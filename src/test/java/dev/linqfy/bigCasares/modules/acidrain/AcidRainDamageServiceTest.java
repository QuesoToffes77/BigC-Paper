package dev.linqfy.bigCasares.modules.acidrain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainDamageServiceTest {

    private static final Instant START = Instant.parse("2026-08-07T12:00:00Z");

    @Test
    void damagesOnlyEligibleExposedPlayersAtTheLevelInterval() {
        AcidRainDamageService service = new AcidRainDamageService(AcidRainSettings.safeDefaults());
        AcidRainSnapshot storm = active(AcidRainLevel.ACID, START);
        AcidRainExposureSnapshot player = exposed("player-1");

        AcidRainDamageDecision first = service.evaluate(player, storm, START);
        AcidRainDamageDecision duplicate = service.evaluate(player, storm, START.plusSeconds(4));
        AcidRainDamageDecision afterInterval = service.evaluate(player, storm, START.plusSeconds(5));

        assertTrue(first.applies());
        assertEquals(1.0, first.damage());
        assertFalse(duplicate.applies());
        assertEquals(AcidRainDamageSkipReason.INTERVAL, duplicate.skipReason());
        assertTrue(afterInterval.applies());
    }

    @Test
    void shelterProtectionBypassOfflineAndExcludedWorldPreventDamage() {
        AcidRainDamageService service = new AcidRainDamageService(AcidRainSettings.safeDefaults());
        AcidRainSnapshot storm = active(AcidRainLevel.TOXIC, START);

        assertEquals(AcidRainDamageSkipReason.SHELTERED,
            service.evaluate(exposed("sheltered").withExposed(false), storm, START).skipReason());
        assertEquals(AcidRainDamageSkipReason.PROTECTED,
            service.evaluate(exposed("protected").withProtectionPercent(100.0), storm, START).skipReason());
        assertEquals(AcidRainDamageSkipReason.BYPASS,
            service.evaluate(exposed("bypass").withBypass(true), storm, START).skipReason());
        assertEquals(AcidRainDamageSkipReason.OFFLINE,
            service.evaluate(exposed("offline").withOnline(false), storm, START).skipReason());
        assertEquals(AcidRainDamageSkipReason.WORLD,
            service.evaluate(exposed("excluded").withWorldName("world_nether"), storm, START).skipReason());
    }

    @Test
    void partialProtectionReducesDamageAndIsClamped() {
        AcidRainDamageService service = new AcidRainDamageService(AcidRainSettings.safeDefaults());
        AcidRainSnapshot storm = active(AcidRainLevel.CHEMICAL, START);

        AcidRainDamageDecision decision = service.evaluate(
            exposed("player-2").withProtectionPercent(25.0), storm, START);
        AcidRainDamageDecision overProtected = service.evaluate(
            exposed("player-3").withProtectionPercent(180.0), storm, START);

        assertTrue(decision.applies());
        assertEquals(2.25, decision.damage());
        assertFalse(overProtected.applies());
        assertEquals(AcidRainDamageSkipReason.PROTECTED, overProtected.skipReason());
    }

    private static AcidRainSnapshot active(AcidRainLevel level, Instant at) {
        return new AcidRainSnapshot(
            AcidRainState.ACTIVE,
            level,
            at,
            at.plusSeconds(300),
            300,
            300,
            AcidRainSettings.safeDefaults().worlds()
        );
    }

    private static AcidRainExposureSnapshot exposed(String playerId) {
        return new AcidRainExposureSnapshot(playerId, "world", true, true, false, 0.0);
    }
}
