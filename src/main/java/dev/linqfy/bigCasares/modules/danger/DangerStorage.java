package dev.linqfy.bigCasares.modules.danger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DangerStorage {
    Optional<DangerState> load(UUID playerId);
    void save(DangerState state);
    List<DangerState> loadAll();
}
