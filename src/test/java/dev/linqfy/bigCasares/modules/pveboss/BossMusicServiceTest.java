package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossMusicServiceTest {

    @Test
    void doesNotRestartTheSameTrackAndTransitionsOnceWhenThePhaseChanges() {
        RecordingAudioGateway gateway = new RecordingAudioGateway();
        BossMusicService service = new BossMusicService(gateway);
        UUID bossId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        BossMusicTrack phaseOne = track("phase-1");
        BossMusicTrack phaseTwo = track("phase-2");

        service.update(bossId, playerId, phaseOne, 10.0, 64.0, true, true);
        service.update(bossId, playerId, phaseOne, 11.0, 64.0, true, true);
        service.update(bossId, playerId, phaseTwo, 11.0, 64.0, true, true);
        service.update(bossId, playerId, phaseTwo, 12.0, 64.0, true, true);

        assertEquals(List.of("start:phase-1:CUSTOM", "transition:phase-2:CUSTOM"), gateway.events);
    }

    @Test
    void stopsMusicOnceWhenThePlayerLeavesTheBossRadius() {
        RecordingAudioGateway gateway = new RecordingAudioGateway();
        BossMusicService service = new BossMusicService(gateway);
        UUID bossId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        service.update(bossId, playerId, track("phase-1"), 10.0, 64.0, true, true);
        service.update(bossId, playerId, track("phase-1"), 65.0, 64.0, true, true);
        service.update(bossId, playerId, track("phase-1"), 70.0, 64.0, true, true);

        assertEquals(List.of("start:phase-1:CUSTOM", "stop:LEFT_RADIUS"), gateway.events);
        assertTrue(service.activeTrack(playerId).isEmpty());
    }

    @Test
    void usesTheVanillaFallbackWhenTheResourcePackIsUnavailable() {
        RecordingAudioGateway gateway = new RecordingAudioGateway();
        BossMusicService service = new BossMusicService(gateway);
        UUID playerId = UUID.randomUUID();

        service.update(UUID.randomUUID(), playerId, track("phase-1"), 4.0, 64.0, false, true);

        assertEquals(BossMusicSource.FALLBACK, service.activeTrack(playerId).orElseThrow().source());
        assertEquals("minecraft:music_disc.5", service.activeTrack(playerId).orElseThrow().playbackSoundKey());
    }

    private static BossMusicTrack track(String id) {
        return new BossMusicTrack(id, "bigcasares:music." + id, "minecraft:music_disc.5");
    }

    private static final class RecordingAudioGateway implements BossAudioGateway {
        private final List<String> events = new ArrayList<>();

        @Override
        public void startMusic(UUID playerId, BossMusicTrack track) {
            events.add("start:" + track.id() + ":" + track.source());
        }

        @Override
        public void transitionMusic(UUID playerId, BossMusicTrack track) {
            events.add("transition:" + track.id() + ":" + track.source());
        }

        @Override
        public void stopMusic(UUID playerId, MusicStopReason reason) {
            events.add("stop:" + reason);
        }
    }
}
