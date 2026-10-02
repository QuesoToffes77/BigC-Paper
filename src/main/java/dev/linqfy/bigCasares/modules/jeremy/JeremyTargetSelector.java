package dev.linqfy.bigCasares.modules.jeremy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.random.RandomGenerator;

final class JeremyTargetSelector {
    private final JeremyTargetingSettings settings;
    private final RandomGenerator random;

    JeremyTargetSelector(JeremyTargetingSettings settings, RandomGenerator random) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.random = Objects.requireNonNull(random, "random");
    }

    Optional<UUID> select(List<JeremyTargetCandidate> candidates, UUID lastTargetUuid) {
        List<JeremyTargetCandidate> eligible = candidates == null
            ? new ArrayList<>()
            : candidates.stream().filter(this::eligible).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        if (settings.avoidLastTarget() && eligible.size() > 1 && lastTargetUuid != null) {
            eligible.removeIf(candidate -> lastTargetUuid.equals(candidate.playerUuid()));
        }
        return Optional.of(eligible.get(random.nextInt(eligible.size())).playerUuid());
    }

    boolean eligible(JeremyTargetCandidate candidate) {
        if (candidate == null || candidate.playerUuid() == null || !candidate.online() || candidate.dead()
            || candidate.npc() || !candidate.loaded() || !settings.allowsWorld(candidate.worldName())) {
            return false;
        }
        return switch (candidate.mode()) {
            case SURVIVAL -> settings.survival();
            case ADVENTURE -> settings.adventure();
            case CREATIVE -> settings.creative();
            case SPECTATOR -> settings.spectator();
        };
    }
}

record JeremyTargetCandidate(
    UUID playerUuid,
    boolean online,
    boolean dead,
    boolean npc,
    boolean loaded,
    JeremyPlayerMode mode,
    String worldName
) {
    JeremyTargetCandidate {
        mode = mode == null ? JeremyPlayerMode.SPECTATOR : mode;
    }
}

enum JeremyPlayerMode {
    SURVIVAL,
    ADVENTURE,
    CREATIVE,
    SPECTATOR
}
