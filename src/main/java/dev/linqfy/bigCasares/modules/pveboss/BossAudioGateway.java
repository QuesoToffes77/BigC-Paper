package dev.linqfy.bigCasares.modules.pveboss;

import java.util.UUID;

public interface BossAudioGateway {

    void startMusic(UUID playerId, BossMusicTrack track);

    void transitionMusic(UUID playerId, BossMusicTrack track);

    void stopMusic(UUID playerId, MusicStopReason reason);
}
