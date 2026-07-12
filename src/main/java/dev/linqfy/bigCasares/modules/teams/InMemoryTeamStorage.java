package dev.linqfy.bigCasares.modules.teams;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryTeamStorage implements TeamStorage {

    private volatile Map<TeamId, Team> teams = Map.of();

    @Override
    public Collection<Team> findAll() {
        return List.copyOf(teams.values());
    }

    @Override
    public Optional<Team> findById(TeamId teamId) {
        return Optional.ofNullable(teams.get(teamId));
    }

    @Override
    public synchronized void save(Team team) {
        Objects.requireNonNull(team, "team");
        LinkedHashMap<TeamId, Team> updated = new LinkedHashMap<>(teams);
        updated.put(team.id(), team);
        teams = Map.copyOf(updated);
    }

    @Override
    public synchronized void delete(TeamId teamId) {
        Objects.requireNonNull(teamId, "teamId");
        LinkedHashMap<TeamId, Team> updated = new LinkedHashMap<>(teams);
        updated.remove(teamId);
        teams = Map.copyOf(updated);
    }
}
