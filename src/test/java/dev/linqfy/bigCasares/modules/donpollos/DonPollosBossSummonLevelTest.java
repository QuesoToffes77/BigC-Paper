package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ritual -> desconexion -> spawn: el nivel se captura al poner el bloque y se resuelve despues con los cercanos. */
class DonPollosBossSummonLevelTest {

    private static final NamespacedKey WINS = NamespacedKey.fromString("test:boss_ganadas");
    private static final org.bukkit.World WORLD = (org.bukkit.World) Proxy.newProxyInstance(
        DonPollosBossSummonLevelTest.class.getClassLoader(), new Class<?>[]{org.bukkit.World.class},
        (proxy, method, args) -> switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "fakeWorld";
            default -> throw new UnsupportedOperationException(method.getName());
        });
    private final Location spawn = new Location(WORLD, 0, 64, 0);

    private static PersistentDataContainer pdc(int wins) {
        return (PersistentDataContainer) Proxy.newProxyInstance(DonPollosBossSummonLevelTest.class.getClassLoader(),
            new Class<?>[]{PersistentDataContainer.class}, (proxy, method, args) -> {
                if (method.getName().equals("getOrDefault")) {
                    return wins;
                }
                throw new UnsupportedOperationException(method.getName());
            });
    }

    private static Player player(int wins, double x, GameMode mode) {
        return (Player) Proxy.newProxyInstance(DonPollosBossSummonLevelTest.class.getClassLoader(),
            new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
                case "isDead" -> false;
                case "getGameMode" -> mode;
                case "getLocation" -> new Location(WORLD, x, 64, 0);
                case "getPersistentDataContainer" -> pdc(wins);
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    @Test
    void summonerLevelSurvivesLogout() {
        int captured = DonPollosBossLevels.winsOf(pdc(6), WINS); // al poner el bloque
        assertEquals(6, captured);
        // al terminar el ritual ya no esta conectado: no hay nadie en la lista
        assertEquals(6, DonPollosBossLevels.levelAt(spawn, captured, List.of(), WINS, 48));
        // conectado: el mismo nivel
        assertEquals(6, DonPollosBossLevels.levelAt(spawn, captured, List.of(player(6, 2, GameMode.SURVIVAL)), WINS, 48));
    }

    @Test
    void strongerNearbyPlayerRaisesTheLevel() {
        assertEquals(9, DonPollosBossLevels.levelAt(spawn, 6, List.of(player(9, 10, GameMode.ADVENTURE)), WINS, 48));
    }

    @Test
    void farCreativeAndSpectatorPlayersDoNotCount() {
        List<Player> players = List.of(player(9, 60, GameMode.SURVIVAL), player(9, 1, GameMode.CREATIVE),
            player(9, 1, GameMode.SPECTATOR));
        assertEquals(6, DonPollosBossLevels.levelAt(spawn, 6, players, WINS, 48));
    }

    @Test
    void aStrongerAttackerRaisesTheBossButNeverLowersIt() {
        assertEquals(8, DonPollosBossLevels.levelAfterHit(0, 8));
        assertEquals(8, DonPollosBossLevels.levelAfterHit(8, 3));
        assertEquals(8, DonPollosBossLevels.levelAfterHit(8, 8));
    }
}
