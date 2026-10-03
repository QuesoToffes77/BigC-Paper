package dev.linqfy.bigCasares.modules.acidrain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainLevelTest {

    @Test
    void parsesSupportedLevelsOnly() {
        assertEquals(AcidRainLevel.ACID, AcidRainLevel.parse("acid").orElseThrow());
        assertEquals(AcidRainLevel.TOXIC, AcidRainLevel.parse("TOXIC").orElseThrow());
        assertEquals(AcidRainLevel.CHEMICAL, AcidRainLevel.parse("chemical").orElseThrow());
        assertTrue(AcidRainLevel.parse("unknown").isEmpty());
    }

    @Test
    void escalatesOnlyThroughConfiguredStepsAndStopsAtMaxLevel() {
        Instant start = Instant.parse("2026-08-07T12:00:00Z");
        AcidRainSettings settings = AcidRainSettings.safeDefaults()
            .withDuration(new AcidRainDurationSettings(120, 120, 5))
            .withLevels(Map.of(
                AcidRainLevel.ACID, new AcidRainLevelSettings(1.0, 5, 1.0, List.of()),
                AcidRainLevel.TOXIC, new AcidRainLevelSettings(2.0, 3, 2.0, List.of()),
                AcidRainLevel.CHEMICAL, new AcidRainLevelSettings(3.0, 2, 3.0, List.of())
            ))
            .withEscalation(new AcidRainEscalationSettings(true, List.of(
                new AcidRainEscalationStep(AcidRainLevel.TOXIC, 30),
                new AcidRainEscalationStep(AcidRainLevel.CHEMICAL, 90)
            )));
        AcidRainStormService service = new AcidRainStormService(settings, Clock.fixed(start, ZoneOffset.UTC));

        service.startNow(AcidRainLevel.ACID, start);
        service.tick(start.plusSeconds(29));
        assertEquals(AcidRainLevel.ACID, service.snapshot(start.plusSeconds(29)).level());

        service.tick(start.plusSeconds(30));
        assertEquals(AcidRainLevel.TOXIC, service.snapshot(start.plusSeconds(30)).level());

        service.tick(start.plusSeconds(90));
        service.tick(start.plusSeconds(150));
        assertEquals(AcidRainLevel.CHEMICAL, service.snapshot(start.plusSeconds(150)).level());
    }

    @Test
    void defaultDestructionIntensityIncreasesFromAcidToChemical() {
        Map<AcidRainLevel, AcidRainLevelSettings> levels = AcidRainSettings.safeDefaults().levels();

        AcidRainLevelDestructionSettings acid = levels.get(AcidRainLevel.ACID).destruction();
        AcidRainLevelDestructionSettings toxic = levels.get(AcidRainLevel.TOXIC).destruction();
        AcidRainLevelDestructionSettings chemical = levels.get(AcidRainLevel.CHEMICAL).destruction();

        // Faster cycles: ACID slowest, CHEMICAL fastest.
        assertTrue(acid.intervalTicks() > toxic.intervalTicks());
        assertTrue(toxic.intervalTicks() > chemical.intervalTicks());
        // More candidates per cycle: ACID fewest, CHEMICAL most.
        assertTrue(acid.candidatesPerCycle() < toxic.candidatesPerCycle());
        assertTrue(toxic.candidatesPerCycle() < chemical.candidatesPerCycle());
        // Damage and contamination also escalate.
        assertTrue(levels.get(AcidRainLevel.ACID).damage() < levels.get(AcidRainLevel.TOXIC).damage());
        assertTrue(levels.get(AcidRainLevel.TOXIC).damage() < levels.get(AcidRainLevel.CHEMICAL).damage());
    }

    @Test
    void disabledEscalationKeepsInitialLevel() {
        Instant start = Instant.parse("2026-08-07T12:00:00Z");
        AcidRainSettings settings = AcidRainSettings.safeDefaults()
            .withDuration(new AcidRainDurationSettings(120, 120, 5))
            .withEscalation(new AcidRainEscalationSettings(false, List.of(
                new AcidRainEscalationStep(AcidRainLevel.CHEMICAL, 1)
            )));
        AcidRainStormService service = new AcidRainStormService(settings, Clock.fixed(start, ZoneOffset.UTC));

        service.startNow(AcidRainLevel.ACID, start);
        service.tick(start.plusSeconds(60));

        assertFalse(settings.escalation().enabled() && settings.escalation().steps().isEmpty());
        assertEquals(AcidRainLevel.ACID, service.snapshot(start.plusSeconds(60)).level());
    }
}
