package dev.linqfy.bigCasares.modules.acidrain;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AcidRainDamageService {
    private AcidRainSettings settings;
    private final Map<String, Instant> lastDamageAt = new LinkedHashMap<>();

    public AcidRainDamageService(AcidRainSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public synchronized AcidRainDamageDecision evaluate(
        AcidRainExposureSnapshot player,
        AcidRainSnapshot storm,
        Instant now
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(storm, "storm");
        Objects.requireNonNull(now, "now");

        if (!settings.damage().enabled()) {
            return AcidRainDamageDecision.skipped(AcidRainDamageSkipReason.DISABLED);
        }
        if (storm.state() != AcidRainState.ACTIVE) {
            return AcidRainDamageDecision.skipped(AcidRainDamageSkipReason.STORM);
        }
        if (!storm.worlds().isAffected(player.worldName())) {
            return AcidRainDamageDecision.skipped(AcidRainDamageSkipReason.WORLD);
        }
        if (!player.online()) {
            return AcidRainDamageDecision.skipped(AcidRainDamageSkipReason.OFFLINE);
        }
        if (player.bypass()) {
            return AcidRainDamageDecision.skipped(AcidRainDamageSkipReason.BYPASS);
        }
        if (!player.exposed()) {
            return AcidRainDamageDecision.skipped(AcidRainDamageSkipReason.SHELTERED);
        }
        double protection = clamp(player.protectionPercent(), 0.0, 100.0);
        if (protection >= 100.0) {
            return AcidRainDamageDecision.skipped(AcidRainDamageSkipReason.PROTECTED);
        }

        AcidRainLevelSettings levelSettings = settings.settingsFor(storm.level());
        Instant last = lastDamageAt.get(player.playerId());
        int interval = Math.max(1, levelSettings.intervalSeconds());
        if (last != null && Duration.between(last, now).toSeconds() < interval) {
            return AcidRainDamageDecision.skipped(AcidRainDamageSkipReason.INTERVAL);
        }

        double damage = Math.max(0.0, levelSettings.damage()) * (1.0 - protection / 100.0);
        if (damage <= 0.0) {
            return AcidRainDamageDecision.skipped(AcidRainDamageSkipReason.PROTECTED);
        }
        lastDamageAt.put(player.playerId(), now);
        return AcidRainDamageDecision.applied(damage, levelSettings.contaminationIntensity());
    }

    public synchronized void updateSettings(AcidRainSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public synchronized void clear() {
        lastDamageAt.clear();
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

record AcidRainExposureSnapshot(
    String playerId,
    String worldName,
    boolean online,
    boolean exposed,
    boolean bypass,
    double protectionPercent
) {
    AcidRainExposureSnapshot withWorldName(String worldName) {
        return new AcidRainExposureSnapshot(playerId, worldName, online, exposed, bypass, protectionPercent);
    }

    AcidRainExposureSnapshot withOnline(boolean online) {
        return new AcidRainExposureSnapshot(playerId, worldName, online, exposed, bypass, protectionPercent);
    }

    AcidRainExposureSnapshot withExposed(boolean exposed) {
        return new AcidRainExposureSnapshot(playerId, worldName, online, exposed, bypass, protectionPercent);
    }

    AcidRainExposureSnapshot withBypass(boolean bypass) {
        return new AcidRainExposureSnapshot(playerId, worldName, online, exposed, bypass, protectionPercent);
    }

    AcidRainExposureSnapshot withProtectionPercent(double protectionPercent) {
        return new AcidRainExposureSnapshot(playerId, worldName, online, exposed, bypass, protectionPercent);
    }
}

record AcidRainDamageDecision(boolean applies, double damage, double contaminationIntensity, AcidRainDamageSkipReason skipReason) {
    static AcidRainDamageDecision applied(double damage, double contaminationIntensity) {
        return new AcidRainDamageDecision(true, damage, contaminationIntensity, AcidRainDamageSkipReason.NONE);
    }

    static AcidRainDamageDecision skipped(AcidRainDamageSkipReason reason) {
        return new AcidRainDamageDecision(false, 0.0, 0.0, reason);
    }
}

enum AcidRainDamageSkipReason {
    NONE,
    DISABLED,
    STORM,
    WORLD,
    OFFLINE,
    BYPASS,
    SHELTERED,
    PROTECTED,
    INTERVAL
}
