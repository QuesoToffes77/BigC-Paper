package dev.linqfy.bigCasares.modules.teams;

import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BukkitTeamNamePresentationGatewayTest {

    @Test
    void clearPlayerRemovesOnlyBigCasaresScoreboardEntries() {
        ScoreboardFixture fixture = new ScoreboardFixture();
        fixture.team("external_plugin").entries.add("Casares");
        fixture.team("bc_old").entries.add("Casares");

        ManagedScoreboardTeams.removeEntry(fixture.scoreboard, "Casares", null);

        assertTrue(fixture.team("external_plugin").entries.contains("Casares"));
        assertFalse(fixture.team("bc_old").entries.contains("Casares"));
    }

    @Test
    void managedCleanupKeepsForeignTeamsAndMovesEntryAwayFromOldBigCasaresTeams() {
        ScoreboardFixture fixture = new ScoreboardFixture();
        fixture.team("external_plugin").entries.add("Casares");
        fixture.team("bc_old").entries.add("Casares");
        fixture.team("bc_gds").entries.add("Casares");

        ManagedScoreboardTeams.removeEntry(
            fixture.scoreboard,
            "Casares",
            "bc_gds"
        );

        assertTrue(fixture.team("external_plugin").entries.contains("Casares"));
        assertFalse(fixture.team("bc_old").entries.contains("Casares"));
        assertTrue(fixture.team("bc_gds").entries.contains("Casares"));
    }

    private static final class ScoreboardFixture {
        private final Map<String, TeamState> teams = new LinkedHashMap<>();
        private final Scoreboard scoreboard = proxy(Scoreboard.class, (method, args) -> switch (method.getName()) {
            case "getTeams" -> teams.values().stream().map(state -> state.team).collect(java.util.stream.Collectors.toSet());
            case "getTeam" -> teams.containsKey(args[0]) ? teams.get(args[0]).team : null;
            case "registerNewTeam" -> team((String) args[0]).team;
            default -> defaultValue(method.getReturnType());
        });

        TeamState team(String name) {
            return teams.computeIfAbsent(name, TeamState::new);
        }
    }

    private static final class TeamState {
        private final Set<String> entries = new LinkedHashSet<>();
        private final Team team;

        private TeamState(String name) {
            this.team = proxy(Team.class, (method, args) -> switch (method.getName()) {
                case "getName" -> name;
                case "getEntries" -> Set.copyOf(entries);
                case "hasEntry" -> entries.contains(args[0]);
                case "addEntry" -> {
                    entries.add((String) args[0]);
                    yield null;
                }
                case "removeEntry" -> entries.remove(args[0]);
                default -> defaultValue(method.getReturnType());
            });
        }
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(java.lang.reflect.Method method, Object[] args) throws Throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation invocation) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            (proxy, method, args) -> invocation.invoke(method, args == null ? new Object[0] : args)
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
