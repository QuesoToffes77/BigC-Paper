package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Desconexion adentro del cubo -> restauracion -> reconexion, con un jugador y un mundo falsos. */
class DonPollosPitExitTest {

    private long now = 1_000_000L;
    private final UUID worldId = UUID.randomUUID();
    private final World world = (World) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{World.class},
        (proxy, method, args) -> switch (method.getName()) {
            case "getUID" -> worldId;
            case "getBlockAt" -> openBlock();
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "FakeWorld";
            default -> throw new UnsupportedOperationException(method.getName());
        });
    private final Map<UUID, DonPollosBoss> bosses = new HashMap<>();
    private final List<Runnable> scheduled = new ArrayList<>();
    private final NamespacedKey key = new NamespacedKey("bigcasares", "pozo_salida");
    private final DonPollosPitExit exit = new DonPollosPitExit(bosses, key, scheduled::add,
        id -> id.equals(worldId) ? world : null, () -> now);

    private final Map<NamespacedKey, Object> data = new HashMap<>();
    private Location playerAt = new Location(world, 40.5, 66, -8.5);
    private boolean teleportWorks = true;
    private final List<Location> teleports = new ArrayList<>();
    private final Player player = fakePlayer();

    private Block openBlock() {
        return (Block) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Block.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "isPassable" -> true;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private DonPollosBoss bossWithPit() {
        DonPollosBoss boss = new DonPollosBoss(null);
        boss.cube = new DonPollosCubeBattle(List.of(new DonPollosCubeRound(1, 1.0, 1.0, 0)), 5, 20, 0);
        boss.pits.add(new DonPollosBossPit(world, 2, -1, 70, 64, List.of()));
        bosses.put(UUID.randomUUID(), boss);
        return boss;
    }

    private void quit() {
        exit.onQuit(new PlayerQuitEvent(player, "bye"));
    }

    @Test
    void quittingInsideAPitLeavesAMarkerWithTheSurfaceExit() {
        bossWithPit();
        quit();

        String saved = (String) data.get(key);
        assertNotNull(saved);
        DonPollosPitExit.Marker marker = DonPollosPitExit.Marker.decode(saved);
        assertEquals(70, marker.feet(), "la superficie original del pozo");
        assertEquals(40.5, marker.x());
        assertEquals(-8.5, marker.z());
        assertEquals(now, marker.savedAt());
    }

    @Test
    void noMarkerWhenTheCubeBattleIsNotActive() {
        bossWithPit().cube = null;
        quit();
        assertTrue(data.isEmpty());
    }

    @Test
    void quittingOutsideAnyPitWritesNothing() {
        bossWithPit();
        playerAt = new Location(world, 500, 80, 500);
        quit();
        assertTrue(data.isEmpty());
    }

    @Test
    void afterTheRestorationRejoiningTeleportsToTheExitAndClearsTheMarker() {
        DonPollosBoss boss = bossWithPit();
        quit();
        boss.pits.clear();  // el cubo se restauro con el jugador desconectado

        exit.onJoin(new PlayerJoinEvent(player, "hi"));
        assertEquals(1, scheduled.size(), "se corre un tick despues");
        scheduled.get(0).run();

        assertEquals(1, teleports.size());
        assertEquals(70, teleports.get(0).getY());
        assertNull(data.get(key));
    }

    @Test
    void aFailedTeleportKeepsTheMarkerForTheNextLogin() {
        DonPollosBoss boss = bossWithPit();
        quit();
        boss.pits.clear();
        teleportWorks = false;

        exit.recover(player);
        assertEquals(1, teleports.size());
        assertNotNull(data.get(key));

        teleportWorks = true;
        exit.recover(player);
        assertNull(data.get(key));
    }

    @Test
    void theMarkerSurvivesANewListenerInstanceLikeARestart() {
        DonPollosBoss boss = bossWithPit();
        quit();
        boss.pits.clear();

        new DonPollosPitExit(bosses, key, scheduled::add, id -> world, () -> now).recover(player);
        assertEquals(1, teleports.size());
    }

    @Test
    void rejoiningInsideAStillActivePitJustDropsTheMarker() {
        bossWithPit();
        quit();

        exit.recover(player);
        assertTrue(teleports.isEmpty());
        assertNull(data.get(key));
    }

    @Test
    void aWorldThatIsNotLoadedKeepsTheMarker() {
        DonPollosBoss boss = bossWithPit();
        quit();
        boss.pits.clear();

        new DonPollosPitExit(bosses, key, scheduled::add, id -> null, () -> now).recover(player);
        assertTrue(teleports.isEmpty());
        assertTrue(data.containsKey(key));
    }

    @Test
    void aPlayerMovedElsewhereWhileOfflineIsNotTeleported() {
        DonPollosBoss boss = bossWithPit();
        quit();
        boss.pits.clear();
        playerAt = new Location(world, 500, 80, 500);

        exit.recover(player);
        assertTrue(teleports.isEmpty());
        assertFalse(data.containsKey(key));
    }

    @Test
    void aPlayerAlreadyAboveTheSurfaceOfTheHoleIsNotTeleported() {
        DonPollosBoss boss = bossWithPit();
        quit();
        boss.pits.clear();
        playerAt = new Location(world, 40.5, 71, -8.5);

        exit.recover(player);
        assertTrue(teleports.isEmpty());
        assertFalse(data.containsKey(key));
    }

    @Test
    void markersOlderThanSevenDaysAreDropped() {
        DonPollosBoss boss = bossWithPit();
        quit();
        boss.pits.clear();
        now += DonPollosPitExit.MAX_AGE_MILLIS + 1;

        exit.recover(player);
        assertTrue(teleports.isEmpty());
        assertFalse(data.containsKey(key));
    }

    // ------------------------------------------------------------ falsos

    private Player fakePlayer() {
        PersistentDataContainer pdc = (PersistentDataContainer) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{PersistentDataContainer.class}, (proxy, method, args) -> switch (method.getName()) {
                case "has" -> data.containsKey((NamespacedKey) args[0]);
                case "get" -> data.get((NamespacedKey) args[0]);
                case "set" -> {
                    data.put((NamespacedKey) args[0], args[2]);
                    yield null;
                }
                case "remove" -> {
                    data.remove((NamespacedKey) args[0]);
                    yield null;
                }
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            });
        return (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getLocation" -> playerAt.clone();
                case "getPersistentDataContainer" -> pdc;
                case "teleport" -> {
                    teleports.add((Location) args[0]);
                    if (teleportWorks) {
                        playerAt = (Location) args[0];
                    }
                    yield teleportWorks;
                }
                case "setFallDistance" -> null;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "FakePlayer";
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }
}
