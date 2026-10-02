package dev.linqfy.bigCasares.modules.acidrain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AcidRainCleanupTest {

    @Test
    void cleanupRemovesStormTemporaryStateAndPresentationHandles() {
        Instant now = Instant.parse("2026-08-07T12:00:00Z");
        AcidRainStormService service = new AcidRainStormService(AcidRainSettings.safeDefaults(), Clock.fixed(now, ZoneOffset.UTC));
        AcidRainPresentationRegistry presentation = new AcidRainPresentationRegistry();

        service.startNow(AcidRainLevel.ACID, now);
        service.contaminate(new AcidRainContamination("player-1", 1.0, now.plusSeconds(30)));
        presentation.ensureBossBar("player-1");
        presentation.ensureBossBar("player-1");
        presentation.ensureBossBar("player-2");

        assertEquals(2, presentation.bossBarCount());

        service.shutdown();
        presentation.clear();

        assertEquals(AcidRainState.INACTIVE, service.snapshot(now).state());
        assertEquals(0, service.temporaryStateCount());
        assertEquals(0, presentation.bossBarCount());
    }

    @Test
    void repeatedStartReloadStopStartDoesNotCreateDuplicatePresentationHandles() {
        AcidRainPresentationRegistry presentation = new AcidRainPresentationRegistry();

        presentation.ensureBossBar("player-1");
        presentation.ensureBossBar("player-1");
        presentation.reload();
        presentation.reload();
        presentation.clear();
        presentation.ensureBossBar("player-1");

        assertEquals(1, presentation.bossBarCount());
    }
}
