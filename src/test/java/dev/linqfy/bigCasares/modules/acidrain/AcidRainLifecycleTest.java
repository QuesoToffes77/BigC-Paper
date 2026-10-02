package dev.linqfy.bigCasares.modules.acidrain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainLifecycleTest {

    private static final Instant START = Instant.parse("2026-08-07T12:00:00Z");

    @Test
    void warningActivatesAndThenEndsInOrder() {
        AcidRainStormService service = new AcidRainStormService(settings(), fixedClock(START));

        AcidRainStartResult warning = service.beginWarning(AcidRainLevel.ACID, START);
        service.tick(START.plusSeconds(29));
        AcidRainSnapshot stillWarning = service.snapshot(START.plusSeconds(29));
        service.tick(START.plusSeconds(30));
        AcidRainSnapshot active = service.snapshot(START.plusSeconds(30));
        service.tick(START.plusSeconds(90));
        AcidRainSnapshot ending = service.snapshot(START.plusSeconds(90));
        service.tick(START.plusSeconds(100));

        assertTrue(warning.started());
        assertEquals(AcidRainState.WARNING, stillWarning.state());
        assertEquals(AcidRainState.ACTIVE, active.state());
        assertEquals(AcidRainState.ENDING, ending.state());
        assertEquals(AcidRainState.INACTIVE, service.snapshot(START.plusSeconds(100)).state());
    }

    @Test
    void rejectsInvalidTransitionsAndDuplicateStarts() {
        AcidRainStormService service = new AcidRainStormService(settings(), fixedClock(START));

        assertFalse(service.activateWarning(START).transitioned());
        assertTrue(service.startNow(AcidRainLevel.TOXIC, START).started());
        assertFalse(service.startNow(AcidRainLevel.CHEMICAL, START.plusSeconds(1)).started());
        assertFalse(service.beginWarning(AcidRainLevel.ACID, START.plusSeconds(2)).started());

        assertEquals(AcidRainState.ACTIVE, service.snapshot(START.plusSeconds(2)).state());
        assertEquals(AcidRainLevel.TOXIC, service.snapshot(START.plusSeconds(2)).level());
    }

    @Test
    void stopMovesThroughEndingAndCleansTemporaryState() {
        AcidRainStormService service = new AcidRainStormService(settings(), fixedClock(START));
        service.startNow(AcidRainLevel.CHEMICAL, START);

        AcidRainTransitionResult stopped = service.stop(START.plusSeconds(5), "admin");
        service.contaminate(new AcidRainContamination("player-1", 2.0, START.plusSeconds(30)));
        service.tick(START.plusSeconds(15));

        assertTrue(stopped.transitioned());
        assertEquals(AcidRainState.INACTIVE, service.snapshot(START.plusSeconds(15)).state());
        assertEquals(0, service.temporaryStateCount());
    }

    private static AcidRainSettings settings() {
        return AcidRainSettings.safeDefaults()
            .withDuration(new AcidRainDurationSettings(60, 60, 10))
            .withWarning(new AcidRainWarningSettings(30, 10, List.of("warning"), AcidRainSoundSettings.disabled(), true))
            .withAutomatic(AcidRainAutomaticSettings.disabled());
    }

    private static Clock fixedClock(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }
}
