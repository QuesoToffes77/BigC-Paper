package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A quien le salen los bichos: los ocupantes reales de los cubos, esten a la distancia que esten del Ravager. */
class DonPollosCubeFighterTest {

    private final List<Player> online = new ArrayList<>();
    private final World world = (World) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{World.class},
        (proxy, method, args) -> switch (method.getName()) {
            case "getPlayers" -> List.copyOf(online);
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "FakeWorld";
            default -> throw new UnsupportedOperationException(method.getName());
        });

    private Player player(Location at, GameMode mode, boolean dead, UUID id) {
        Player p = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getLocation" -> at.clone();
                case "getGameMode" -> mode;
                case "isDead" -> dead;
                case "getUniqueId" -> id;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "FakePlayer";
                default -> throw new UnsupportedOperationException(method.getName());
            });
        online.add(p);
        return p;
    }

    private DonPollosBoss bossWithPit(int chunkX, int chunkZ) {
        DonPollosBoss boss = new DonPollosBoss(null);
        boss.pits.add(new DonPollosBossPit(world, chunkX, chunkZ, 199, 193, List.of()));
        return boss;
    }

    @Test
    void aPlayerInAPitFarFromTheBossIsStillSelected() {
        // el pozo reubicado en una meseta: el Ravager esta a mas de 100 bloques, fuera de la barra y del respaldo
        DonPollosBoss boss = bossWithPit(10, 10);
        Player far = player(new Location(world, 168.5, 194, 168.5), GameMode.SURVIVAL, false, UUID.randomUUID());

        List<Player> occupants = DonPollosBossTerrain.occupants(boss);

        assertEquals(List.of(far), occupants);
        assertSame(far, DonPollosBossTerrain.pickFighter(occupants, 0));
    }

    @Test
    void onlyAttackablePlayersInsideThePitCount() {
        DonPollosBoss boss = bossWithPit(10, 10);
        Location inside = new Location(world, 168.5, 194, 168.5);
        player(inside, GameMode.CREATIVE, false, UUID.randomUUID());
        player(inside, GameMode.SPECTATOR, false, UUID.randomUUID());
        player(inside, GameMode.SURVIVAL, true, UUID.randomUUID());
        player(new Location(world, 0.5, 70, 0.5), GameMode.SURVIVAL, false, UUID.randomUUID());

        assertTrue(DonPollosBossTerrain.occupants(boss).isEmpty());
        assertNull(DonPollosBossTerrain.pickFighter(List.of(), 5), "sin nadie adentro se usa el respaldo");
    }

    @Test
    void fightersRotateInAStableOrderAcrossPits() {
        DonPollosBoss boss = bossWithPit(10, 10);
        boss.pits.add(new DonPollosBossPit(world, 20, 20, 70, 64, List.of()));
        Player a = player(new Location(world, 168.5, 194, 168.5), GameMode.SURVIVAL, false, new UUID(0, 1));
        Player b = player(new Location(world, 328.5, 66, 328.5), GameMode.SURVIVAL, false, new UUID(0, 2));

        List<Player> occupants = DonPollosBossTerrain.occupants(boss);
        assertEquals(2, occupants.size());
        assertSame(a, DonPollosBossTerrain.pickFighter(occupants, 0));
        assertSame(b, DonPollosBossTerrain.pickFighter(occupants, 1));
        assertSame(a, DonPollosBossTerrain.pickFighter(occupants, 2));
    }

    @Test
    void relocationRejectsAbsurdVerticalJumps() {
        assertTrue(DonPollosBossTerrain.reasonableRelocation(70, 70));
        assertTrue(DonPollosBossTerrain.reasonableRelocation(70 + DonPollosBossTerrain.MAX_RELOCATE_VERTICAL, 70));
        assertTrue(DonPollosBossTerrain.reasonableRelocation(70 - DonPollosBossTerrain.MAX_RELOCATE_VERTICAL, 70));
        assertFalse(DonPollosBossTerrain.reasonableRelocation(200, 70), "la meseta 130 bloques arriba");
        assertFalse(DonPollosBossTerrain.reasonableRelocation(30, 70 + DonPollosBossTerrain.MAX_RELOCATE_VERTICAL + 20));
    }
}
