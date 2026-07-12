package dev.linqfy.bigCasares.modules.skillrating;

import java.util.Optional;
import java.util.UUID;

public interface SkillRatingStorage {

    Optional<SkillRatingState> load(UUID playerId);

    void save(SkillRatingState state);

    java.util.List<SkillRatingState> loadAll();
}
