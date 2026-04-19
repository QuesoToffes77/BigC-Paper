package dev.linqfy.bigCasares.modules.missions;

import java.util.Optional;
import java.util.UUID;

public interface MissionStorage {

    Optional<MissionPlayerState> load(UUID playerId);

    void save(MissionPlayerState state);
}
