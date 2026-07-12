package dev.linqfy.bigCasares.modules.pveboss;

import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PaperBossAudioGateway implements BossAudioGateway {
    private final Server server;
    private final Map<UUID, String> playingSounds = new HashMap<>();

    public PaperBossAudioGateway(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public void startMusic(UUID playerId, BossMusicTrack track) {
        play(playerId, track);
    }

    @Override
    public void transitionMusic(UUID playerId, BossMusicTrack track) {
        stopCurrent(playerId);
        play(playerId, track);
    }

    @Override
    public void stopMusic(UUID playerId, MusicStopReason reason) {
        stopCurrent(playerId);
    }

    private void play(UUID playerId, BossMusicTrack track) {
        Player player = server.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        String sound = track.playbackSoundKey();
        player.playSound(player.getLocation(), sound, SoundCategory.MUSIC, 0.8f, 1.0f);
        playingSounds.put(playerId, sound);
    }

    private void stopCurrent(UUID playerId) {
        String sound = playingSounds.remove(playerId);
        Player player = server.getPlayer(playerId);
        if (sound != null && player != null) {
            player.stopSound(sound, SoundCategory.MUSIC);
        }
    }
}
