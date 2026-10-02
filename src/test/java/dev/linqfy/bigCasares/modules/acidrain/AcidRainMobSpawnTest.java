package dev.linqfy.bigCasares.modules.acidrain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainMobSpawnTest {

    private static final Instant START = Instant.parse("2026-08-14T12:00:00Z");

    @Test
    void inactiveWarningAndEndingNeverSpawn() {
        AcidRainMobSpawnService service = newService(settings(10, 2, 5));
        service.start();

        assertFalse(cycle(service, snapshot(AcidRainState.INACTIVE, AcidRainLevel.TOXIC), 0).spawnAllowed());
        assertFalse(cycle(service, snapshot(AcidRainState.WARNING, AcidRainLevel.TOXIC), 0).spawnAllowed());
        assertFalse(cycle(service, snapshot(AcidRainState.ENDING, AcidRainLevel.TOXIC), 0).spawnAllowed());
    }

    @Test
    void acidAndChemicalActiveCanSpawnAfterTheirInterval() {
        AcidRainMobSpawnService service = newService(settings(10, 2, 5));
        service.start();

        AcidRainMobCycleDecision acid = cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.ACID), 0);
        AcidRainMobCycleDecision chemical = cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.CHEMICAL), 0);

        assertTrue(acid.spawnAllowed());
        assertTrue(chemical.spawnAllowed());
    }

    @Test
    void toxicActiveSpawnsAfterTheIntervalElapses() {
        AcidRainMobSpawnService service = newService(settings(20, 2, 5));
        service.start();

        AcidRainMobCycleDecision first = cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0);
        assertFalse(first.spawnAllowed());
        assertEquals(AcidRainMobSkipReason.INTERVAL, first.reason());

        AcidRainMobCycleDecision second = cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0);
        assertTrue(second.spawnAllowed());
        assertEquals(2, second.attempts());
    }

    @Test
    void maxActiveIsNeverExceeded() {
        AcidRainMobSpawnService service = newService(settings(10, 2, 5));
        service.start();
        AcidRainSnapshot toxic = snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC);

        AcidRainMobCycleDecision empty = cycle(service, toxic, 0);
        assertTrue(empty.spawnAllowed());
        assertEquals(2, empty.attempts());

        AcidRainMobCycleDecision nearCap = cycle(service, toxic, 4);
        assertTrue(nearCap.spawnAllowed());
        assertEquals(1, nearCap.attempts());

        AcidRainMobCycleDecision full = cycle(service, toxic, 5);
        assertFalse(full.spawnAllowed());
        assertEquals(AcidRainMobSkipReason.CAPACITY, full.reason());
    }

    @Test
    void stoppedSchedulerNeverSpawns() {
        AcidRainMobSpawnService service = newService(settings(10, 2, 5));

        AcidRainMobCycleDecision beforeStart = cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0);
        assertFalse(beforeStart.spawnAllowed());
        assertEquals(AcidRainMobSkipReason.STOPPED, beforeStart.reason());

        assertTrue(service.start());
        assertTrue(service.stop());
        assertFalse(service.isRunning());

        AcidRainMobCycleDecision afterStop = cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0);
        assertFalse(afterStop.spawnAllowed());
        assertEquals(AcidRainMobSkipReason.STOPPED, afterStop.reason());
    }

    @Test
    void startIsIdempotentAndSecondToxicStormStartsFresh() {
        AcidRainMobSpawnService service = newService(settings(20, 2, 5));

        assertTrue(service.start());
        assertFalse(service.start());

        // First storm: interval gate applies from a clean start.
        assertFalse(cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0).spawnAllowed());
        assertTrue(cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0).spawnAllowed());

        // Storm ends.
        assertTrue(service.stop());
        assertFalse(service.stop());

        // Second storm: fresh scheduler, fresh interval, single instance.
        assertTrue(service.start());
        assertFalse(service.start());
        assertFalse(cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0).spawnAllowed());
        assertTrue(cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0).spawnAllowed());
    }

    @Test
    void reloadKeepsSingleSchedulerWithoutMultiplyingAttempts() {
        AcidRainMobSpawnService service = newService(settings(10, 2, 5));
        assertTrue(service.start());

        // Simulate /acidrain reload: same instance, new settings.
        service.updateSettings(settings(10, 2, 5));

        assertTrue(service.isRunning());
        assertFalse(service.start());

        AcidRainMobCycleDecision decision = cycle(service, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0);
        assertTrue(decision.spawnAllowed());
        assertEquals(2, decision.attempts());
    }

    @Test
    void disabledOrZeroLimitSettingsNeverSpawn() {
        AcidRainMobSpawnService disabled = newService(AcidRainMobSettings.disabled());
        disabled.start();
        AcidRainMobCycleDecision disabledDecision =
            cycle(disabled, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0);
        assertFalse(disabledDecision.spawnAllowed());
        assertEquals(AcidRainMobSkipReason.DISABLED, disabledDecision.reason());

        AcidRainMobSettings zeroRadius = new AcidRainMobSettings(true, 10, 2, 5, 0,
            AcidRainMobSettings.defaults().types());
        AcidRainMobSpawnService noRadius = newService(zeroRadius);
        noRadius.start();
        assertFalse(cycle(noRadius, snapshot(AcidRainState.ACTIVE, AcidRainLevel.TOXIC), 0).spawnAllowed());
    }

    @Test
    void pickTypeRespectsEnabledTypesAndWeights() {
        AcidRainMobSettings onlyCrawler = new AcidRainMobSettings(true, 10, 2, 5, 24,
            Map.of(AcidRainMobType.CRAWLER, true, AcidRainMobType.BRUTE, false, AcidRainMobType.SPITTER, false));
        AcidRainMobSpawnService service = new AcidRainMobSpawnService(onlyCrawler, new Random(42));
        for (int i = 0; i < 100; i++) {
            assertEquals(AcidRainMobType.CRAWLER, service.pickType());
        }

        AcidRainMobSpawnService all = new AcidRainMobSpawnService(AcidRainMobSettings.defaults(), new Random(7));
        Set<AcidRainMobType> seen = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            seen.add(all.pickType());
        }
        assertTrue(seen.contains(AcidRainMobType.CRAWLER));
        assertTrue(seen.contains(AcidRainMobType.BRUTE));
        assertTrue(seen.contains(AcidRainMobType.SPITTER));
    }

    @Test
    void automaticToxicStormReachesActiveToxicAndSpawns() {
        AcidRainSettings stormSettings = AcidRainSettings.safeDefaults()
            .withDefaultLevel(AcidRainLevel.TOXIC)
            .withDuration(new AcidRainDurationSettings(300, 300, 10))
            .withWarning(new AcidRainWarningSettings(30, 10, List.of(), AcidRainSoundSettings.disabled(), false))
            .withAutomatic(new AcidRainAutomaticSettings(true, 100, 60, 60));
        AcidRainStormService storm = new AcidRainStormService(stormSettings, Clock.fixed(START, ZoneOffset.UTC));
        AcidRainMobSpawnService spawn = newService(settings(10, 2, 5));
        spawn.start();

        assertTrue(storm.attemptAutomaticStart(START, 0).started());
        assertEquals(AcidRainState.WARNING, storm.snapshot(START).state());
        assertFalse(spawn.cycleDecision(storm.snapshot(START), 0, 10).spawnAllowed());

        Instant activeAt = START.plusSeconds(30);
        storm.tick(activeAt);
        AcidRainSnapshot active = storm.snapshot(activeAt);
        assertEquals(AcidRainState.ACTIVE, active.state());
        assertEquals(AcidRainLevel.TOXIC, active.level());
        assertTrue(spawn.cycleDecision(active, 0, 10).spawnAllowed());
    }

    @Test
    void automaticAcidStormSpawnsMobsWhenActive() {
        AcidRainSettings stormSettings = AcidRainSettings.safeDefaults()
            .withDefaultLevel(AcidRainLevel.ACID)
            .withDuration(new AcidRainDurationSettings(300, 300, 10))
            .withWarning(new AcidRainWarningSettings(30, 10, List.of(), AcidRainSoundSettings.disabled(), false))
            .withAutomatic(new AcidRainAutomaticSettings(true, 100, 60, 60));
        AcidRainStormService storm = new AcidRainStormService(stormSettings, Clock.fixed(START, ZoneOffset.UTC));
        AcidRainMobSpawnService spawn = newService(settings(10, 2, 5));
        spawn.start();

        assertTrue(storm.attemptAutomaticStart(START, 0).started());
        Instant activeAt = START.plusSeconds(30);
        storm.tick(activeAt);
        AcidRainSnapshot active = storm.snapshot(activeAt);
        assertEquals(AcidRainState.ACTIVE, active.state());
        assertEquals(AcidRainLevel.ACID, active.level());

        AcidRainMobCycleDecision decision = spawn.cycleDecision(active, 0, 10);
        assertTrue(decision.spawnAllowed());
    }

    private static AcidRainMobSpawnService newService(AcidRainMobSettings settings) {
        return new AcidRainMobSpawnService(settings, new Random(1));
    }

    private static AcidRainMobSettings settings(int intervalTicks, int attempts, int maxActive) {
        return new AcidRainMobSettings(true, intervalTicks, attempts, maxActive, 24,
            AcidRainMobSettings.defaults().types());
    }

    private static AcidRainMobCycleDecision cycle(AcidRainMobSpawnService service, AcidRainSnapshot storm, int active) {
        return service.cycleDecision(storm, active, 10);
    }

    private static AcidRainSnapshot snapshot(AcidRainState state, AcidRainLevel level) {
        return new AcidRainSnapshot(state, level, START, START.plusSeconds(300), 300, 300,
            AcidRainWorldSettings.defaults());
    }
}
