package dev.linqfy.bigCasares.modules.pveboss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class BossMusicService {

    private final BossAudioGateway gateway;
    private final Map<UUID, ActivePlayback> activeByPlayer = new HashMap<>();

    public BossMusicService(BossAudioGateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
    }

    public void update(
        UUID bossInstanceId,
        UUID playerId,
        BossMusicTrack desiredTrack,
        double distance,
        double radius,
        boolean resourcePackLoaded,
        boolean customSoundSupported
    ) {
        Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(desiredTrack, "desiredTrack");
        if (!Double.isFinite(distance) || distance < 0.0) {
            throw new IllegalArgumentException("distance must be finite and non-negative");
        }
        if (!Double.isFinite(radius) || radius <= 0.0) {
            throw new IllegalArgumentException("radius must be finite and positive");
        }

        ActivePlayback active = activeByPlayer.get(playerId);
        if (distance > radius) {
            if (active != null && active.bossInstanceId.equals(bossInstanceId)) {
                activeByPlayer.remove(playerId);
                gateway.stopMusic(playerId, MusicStopReason.LEFT_RADIUS);
            }
            return;
        }

        BossMusicTrack resolved = desiredTrack.resolve(resourcePackLoaded && customSoundSupported);
        if (active == null) {
            activeByPlayer.put(playerId, new ActivePlayback(bossInstanceId, resolved));
            gateway.startMusic(playerId, resolved);
            return;
        }
        if (active.bossInstanceId.equals(bossInstanceId) && active.track.equals(resolved)) {
            return;
        }
        activeByPlayer.put(playerId, new ActivePlayback(bossInstanceId, resolved));
        gateway.transitionMusic(playerId, resolved);
    }

    public void update(
        UUID bossInstanceId,
        UUID playerId,
        BossMusicTrack desiredTrack,
        boolean withinRadius,
        boolean customAudioAvailable
    ) {
        update(
            bossInstanceId,
            playerId,
            desiredTrack,
            withinRadius ? 0.0 : 2.0,
            1.0,
            customAudioAvailable,
            customAudioAvailable
        );
    }

    public void stopBoss(UUID bossInstanceId, MusicStopReason reason) {
        Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        Objects.requireNonNull(reason, "reason");
        ArrayList<UUID> players = new ArrayList<>();
        activeByPlayer.forEach((playerId, active) -> {
            if (active.bossInstanceId.equals(bossInstanceId)) {
                players.add(playerId);
            }
        });
        players.forEach(playerId -> stop(playerId, reason));
    }

    public boolean disconnect(UUID playerId) {
        return stop(Objects.requireNonNull(playerId, "playerId"), MusicStopReason.PLAYER_DISCONNECTED);
    }

    public void stopAll(MusicStopReason reason) {
        Objects.requireNonNull(reason, "reason");
        new ArrayList<>(activeByPlayer.keySet()).forEach(playerId -> stop(playerId, reason));
    }

    public Optional<BossMusicTrack> activeTrack(UUID playerId) {
        ActivePlayback active = activeByPlayer.get(Objects.requireNonNull(playerId, "playerId"));
        return active == null ? Optional.empty() : Optional.of(active.track);
    }

    private boolean stop(UUID playerId, MusicStopReason reason) {
        if (activeByPlayer.remove(playerId) == null) {
            return false;
        }
        gateway.stopMusic(playerId, reason);
        return true;
    }

    private record ActivePlayback(UUID bossInstanceId, BossMusicTrack track) {
    }
}
