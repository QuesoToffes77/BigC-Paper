package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Que bloques puede sacar volando el megasalto: con un mundo falso de piedra hasta y=63 y aire arriba. */
class DonPollosBossShockwavesTest {

    private final Map<String, Block> blocks = new HashMap<>();
    private final World world = (World) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{World.class},
        (proxy, method, args) -> switch (method.getName()) {
            case "getBlockAt" -> args.length == 1
                ? block(((Location) args[0]).getBlockX(), ((Location) args[0]).getBlockY(), ((Location) args[0]).getBlockZ())
                : block((int) args[0], (int) args[1], (int) args[2]);
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "FakeWorld";
            default -> throw new UnsupportedOperationException(method.getName());
        });

    private Block block(int x, int y, int z) {
        return blocks.computeIfAbsent(x + "," + y + "," + z, k -> {
            Material type = x == 100 && y == 63 && z == 100 ? Material.LANTERN : y <= 63 ? Material.STONE : Material.AIR;
            BlockState state = (BlockState) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{BlockState.class}, (p, m, a) -> switch (m.getName()) {
                    case "hashCode" -> System.identityHashCode(p);
                    case "equals" -> p == a[0];
                    default -> throw new UnsupportedOperationException(m.getName());
                });
            return (Block) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Block.class},
                (p, m, a) -> switch (m.getName()) {
                    case "getType" -> type;
                    case "isPassable" -> type == Material.AIR;
                    case "getState" -> state;
                    case "getWorld" -> world;
                    case "getX" -> x;
                    case "getY" -> y;
                    case "getZ" -> z;
                    case "getRelative" -> block(x + (int) a[0], y + (int) a[1], z + (int) a[2]);
                    case "hashCode" -> System.identityHashCode(p);
                    case "equals" -> p == a[0];
                    case "toString" -> "Block(" + x + "," + y + "," + z + ")";
                    default -> throw new UnsupportedOperationException(m.getName());
                });
        });
    }

    /** Sin registro de Bukkit: solido = piedra (el farol no es solido pero tampoco es transitable). */
    private static final Predicate<Block> SOLID = b -> b.getType() == Material.STONE;

    private static DonPollosProtection zone(int minX, int maxX) {
        return (w, x0, x1, y0, y1, z0, z1) -> x0 <= maxX && x1 >= minX;
    }

    @Test
    void withoutProtectionEveryEligibleBlockAroundTheImpactIsACandidate() {
        List<Block> all = DonPollosBossShockwaves.candidates(new Location(world, 100.5, 64, 100.5),
            DonPollosProtection.NONE, b -> false, SOLID);
        assertFalse(all.isEmpty());
    }

    @Test
    void impactInsideAProtectedZoneThrowsNothing() {
        List<Block> inside = DonPollosBossShockwaves.candidates(new Location(world, 5.5, 64, 5.5), zone(0, 10),
            b -> false, SOLID);
        assertTrue(inside.isEmpty());
    }

    @Test
    void impactNextToTheZoneOnlyUsesBlocksFarEnoughAwayForTheFlyingOnesToLandOutside() {
        // zona x 0..10; el impacto en x=22 (los bloques candidatos van de x=19 a 25)
        Location center = new Location(world, 22.5, 64, 5.5);
        DonPollosProtection zone = zone(0, 10);
        List<Block> near = DonPollosBossShockwaves.candidates(center, zone, b -> false, SOLID);
        assertFalse(near.isEmpty());
        for (Block block : near) {
            assertFalse(zone.isProtected(world, block.getX() - DonPollosBossShockwaves.LANDING_MARGIN,
                block.getX() + DonPollosBossShockwaves.LANDING_MARGIN, 0, 255, 0, 255), block.toString());
        }
        // y pegado al borde (x=12) no queda ningun bloque
        assertTrue(DonPollosBossShockwaves.candidates(new Location(world, 12.5, 64, 5.5), zone, b -> false, SOLID).isEmpty());
    }

    @Test
    void blocksOfAnyActiveCubeAreNeverCandidates() {
        Location center = new Location(world, 100.5, 64, 100.5);
        Predicate<Block> cubeWall = b -> b.getX() >= 100;
        List<Block> candidates = DonPollosBossShockwaves.candidates(center, DonPollosProtection.NONE, cubeWall, SOLID);
        assertFalse(candidates.isEmpty());
        assertTrue(candidates.stream().allMatch(b -> b.getX() < 100));
    }

    @Test
    void nonSolidButNonPassableBlocksLikeLanternsAreNotCandidates() {
        List<Block> candidates = DonPollosBossShockwaves.candidates(new Location(world, 100.5, 64, 100.5),
            DonPollosProtection.NONE, b -> false, SOLID);
        assertFalse(candidates.isEmpty());
        assertTrue(candidates.stream().noneMatch(b -> b.getType() == Material.LANTERN));
        assertTrue(candidates.stream().noneMatch(b -> b.getX() == 100 && b.getY() == 63 && b.getZ() == 100));
    }

    @Test
    void landingInsideAZoneOrACubeIsForbiddenAndOutsideIsFine() {
        Block inZone = block(5, 64, 5);
        Block inCube = block(100, 64, 100);
        Block free = block(50, 64, 50);
        DonPollosProtection zone = zone(0, 10);
        Predicate<Block> cube = b -> b.getX() == 100;
        assertTrue(DonPollosShockwaveLanding.forbidden(inZone, zone, cube));
        assertTrue(DonPollosShockwaveLanding.forbidden(inCube, zone, cube));
        assertFalse(DonPollosShockwaveLanding.forbidden(free, zone, cube));
    }
}
