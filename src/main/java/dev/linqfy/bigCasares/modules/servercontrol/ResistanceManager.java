package dev.linqfy.bigCasares.modules.servercontrol;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ResistanceManager {
    private final Map<UUID, java.util.Optional<PotionEffect>> capturedEffects = new HashMap<>();
    private ResistanceLevel level;

    public ResistanceManager(ResistanceLevel initialLevel) {
        this.level = initialLevel;
    }

    public ResistanceLevel level() {
        return level;
    }

    public void changeLevel(ResistanceLevel next, Iterable<? extends Player> players) {
        ResistanceLevel previous = level;
        level = next;
        if (previous == ResistanceLevel.OFF && next != ResistanceLevel.OFF) {
            for (Player player : players) {
                capture(player);
                ensureMinimum(player);
            }
            return;
        }
        if (next == ResistanceLevel.OFF) {
            for (Player player : players) {
                restore(player);
            }
            capturedEffects.clear();
            return;
        }
        for (Player player : players) {
            PotionEffect current = player.getPotionEffect(PotionEffectType.RESISTANCE);
            if (current != null && isGlobalEffect(current, previous)) {
                player.removePotionEffect(PotionEffectType.RESISTANCE);
            }
            ensureMinimum(player);
        }
    }

    public void handlePlayer(Player player) {
        if (level == ResistanceLevel.OFF) {
            return;
        }
        capture(player);
        ensureMinimum(player);
    }

    public void ensureMinimum(Player player) {
        if (level == ResistanceLevel.OFF || !player.isOnline()) {
            return;
        }
        PotionEffect current = player.getPotionEffect(PotionEffectType.RESISTANCE);
        if (current != null && current.getAmplifier() > level.amplifier()) {
            return;
        }
        if (current == null || current.getAmplifier() < level.amplifier() || isExpiring(current)) {
            player.addPotionEffect(globalEffect(), true);
        }
    }

    public void forget(Player player) {
        capturedEffects.remove(player.getUniqueId());
    }

    public void handleDisconnect(Player player) {
        if (level != ResistanceLevel.OFF) {
            restore(player);
        }
        capturedEffects.remove(player.getUniqueId());
    }

    public void shutdown(Iterable<? extends Player> players) {
        if (level == ResistanceLevel.OFF) {
            return;
        }
        for (Player player : players) {
            restore(player);
        }
        capturedEffects.clear();
    }

    private void capture(Player player) {
        capturedEffects.putIfAbsent(
            player.getUniqueId(), java.util.Optional.ofNullable(player.getPotionEffect(PotionEffectType.RESISTANCE))
        );
    }

    private void restore(Player player) {
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        capturedEffects.getOrDefault(player.getUniqueId(), java.util.Optional.empty())
            .ifPresent(effect -> player.addPotionEffect(effect, true));
    }

    private PotionEffect globalEffect() {
        return new PotionEffect(
            PotionEffectType.RESISTANCE, PotionEffect.INFINITE_DURATION, level.amplifier(), false, false, false
        );
    }

    private boolean isExpiring(PotionEffect effect) {
        return effect.getDuration() != PotionEffect.INFINITE_DURATION && effect.getDuration() < 40;
    }

    private boolean isGlobalEffect(PotionEffect effect, ResistanceLevel expectedLevel) {
        return expectedLevel != ResistanceLevel.OFF
            && effect.getAmplifier() == expectedLevel.amplifier()
            && effect.getDuration() == PotionEffect.INFINITE_DURATION
            && !effect.isAmbient() && !effect.hasParticles() && !effect.hasIcon();
    }
}
