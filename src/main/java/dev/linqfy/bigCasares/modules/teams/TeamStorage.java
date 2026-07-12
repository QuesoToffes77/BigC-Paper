package dev.linqfy.bigCasares.modules.teams;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TeamStorage {

    Collection<Team> findAll();

    Optional<Team> findById(TeamId teamId);

    default Optional<Team> findByTag(String tag) {
        if (tag == null) {
            return Optional.empty();
        }
        return findAll().stream()
            .filter(team -> team.tag().equalsIgnoreCase(tag))
            .findFirst();
    }

    default Optional<Team> findByMember(UUID playerId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return findAll().stream()
            .filter(team -> team.containsMember(playerId))
            .findFirst();
    }

    void save(Team team);

    void delete(TeamId teamId);
}
