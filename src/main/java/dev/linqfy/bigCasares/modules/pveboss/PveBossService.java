package dev.linqfy.bigCasares.modules.pveboss;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PveBossService {

    private final Map<UUID, BossAbilityCoordinator> coordinators = new LinkedHashMap<>();

    public synchronized void registerInstance(BossAbilityCoordinator coordinator) {
        Objects.requireNonNull(coordinator, "coordinator");
        UUID bossInstanceId = coordinator.bossInstanceId();
        if (coordinators.containsKey(bossInstanceId)) {
            throw new IllegalStateException("a coordinator already exists for boss " + bossInstanceId);
        }
        coordinators.put(bossInstanceId, coordinator);
    }

    public synchronized Optional<BossAbilityCoordinator> coordinator(UUID bossInstanceId) {
        return Optional.ofNullable(coordinators.get(Objects.requireNonNull(bossInstanceId, "bossInstanceId")));
    }

    public synchronized int activeInstanceCount() {
        return coordinators.size();
    }

    public synchronized boolean removeInstance(UUID bossInstanceId, Instant now) {
        Objects.requireNonNull(bossInstanceId, "bossInstanceId");
        Objects.requireNonNull(now, "now");
        BossAbilityCoordinator coordinator = coordinators.remove(bossInstanceId);
        if (coordinator == null) {
            return false;
        }
        coordinator.cancelAll(now);
        return true;
    }

    public synchronized void shutdown(Instant now) {
        Objects.requireNonNull(now, "now");
        ArrayList<BossAbilityCoordinator> active = new ArrayList<>(coordinators.values());
        coordinators.clear();
        active.forEach(coordinator -> coordinator.cancelAll(now));
    }
}
