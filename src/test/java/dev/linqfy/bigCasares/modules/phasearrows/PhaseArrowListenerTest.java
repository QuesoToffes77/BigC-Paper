package dev.linqfy.bigCasares.modules.phasearrows;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseArrowListenerTest {
    @Test
    void failedTeleportLeavesOriginalHitAndPhaseCountUntouched() {
        Fixture fixture = new Fixture(false, true);
        fixture.listener.onHit(fixture.event);
        assertFalse(fixture.event.isCancelled());
        assertEquals(1, fixture.teleports.get());
        assertEquals(0, fixture.phaseWrites.get());
    }

    @Test
    void successfulTeleportCancelsHitAndCountsOnePhase() {
        Fixture fixture = new Fixture(true, true);
        assertThrows(StopParticle.class, () -> fixture.listener.onHit(fixture.event));
        assertTrue(fixture.event.isCancelled());
        assertEquals(1, fixture.teleports.get());
        assertEquals(1, fixture.phaseWrites.get());
    }

    @Test
    void unloadedChunkStopsPathWithoutLoadingOrTeleporting() {
        Fixture fixture = new Fixture(true, false);
        fixture.listener.onHit(fixture.event);
        assertFalse(fixture.event.isCancelled());
        assertEquals(0, fixture.teleports.get());
        assertEquals(0, fixture.blockReads.get());
    }

    private static final class Fixture {
        final AtomicInteger teleports = new AtomicInteger();
        final AtomicInteger phaseWrites = new AtomicInteger();
        final AtomicInteger blockReads = new AtomicInteger();
        final PhaseArrowListener listener;
        final ProjectileHitEvent event;

        Fixture(boolean teleportSucceeds, boolean loaded) {
            AtomicReference<ProjectileHitEvent> eventRef = new AtomicReference<>();
            World world = proxy(World.class, (method, args) -> {
                if (method.equals("spawnParticle") && teleportSucceeds) throw new StopParticle();
                if (method.equals("isChunkLoaded")) return loaded;
                if (method.equals("getBlockAt")) {
                    blockReads.incrementAndGet();
                    Location at = (Location) args[0];
                    boolean solid = at.getX() < 1.0;
                    return proxy(Block.class, (name, ignored) -> switch (name) {
                        case "getType" -> solid ? Material.STONE : Material.AIR;
                        case "isPassable" -> !solid;
                        default -> null;
                    });
                }
                return null;
            });
            PersistentDataContainer data = proxy(PersistentDataContainer.class, (method, args) -> {
                if (method.equals("getOrDefault")) return 0;
                if (method.equals("set")) phaseWrites.incrementAndGet();
                return null;
            });
            SpectralArrow arrow = proxy(SpectralArrow.class, (method, args) -> switch (method) {
                case "getPersistentDataContainer" -> data;
                case "getVelocity" -> new Vector(1, 0, 0);
                case "getLocation" -> new Location(world, 0, 64, 0);
                case "getWorld" -> world;
                case "teleport" -> {
                    assertFalse(eventRef.get().isCancelled(), "the original hit must survive a failed teleport");
                    teleports.incrementAndGet();
                    yield teleportSucceeds;
                }
                default -> null;
            });
            Block hit = proxy(Block.class, (name, args) -> null);
            listener = new PhaseArrowListener(new PhaseArrowSettings(2.0, 1, false, Set.of()),
                new NamespacedKey("bigcasares", "phase_test"));
            event = new ProjectileHitEvent(arrow, hit);
            eventRef.set(event);
        }
    }

    private static final class StopParticle extends RuntimeException { }

    private interface Call { Object invoke(String method, Object[] args); }

    private static <T> T proxy(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
            (proxy, method, args) -> call.invoke(method.getName(), args)));
    }
}
