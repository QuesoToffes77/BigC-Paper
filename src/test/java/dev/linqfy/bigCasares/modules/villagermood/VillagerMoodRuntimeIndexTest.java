package dev.linqfy.bigCasares.modules.villagermood;

import org.bukkit.NamespacedKey;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillagerMoodRuntimeIndexTest {
    @Test
    void periodicChecksUseLifecycleIndexAndProcessOnlyThirtyTwoPerTick() {
        AtomicInteger visits = new AtomicInteger();
        VillagerMoodRuntime runtime = new VillagerMoodRuntime(VillagerMoodSettings.defaults(),
            new NamespacedKey("bigcasares", "confined"), new NamespacedKey("bigcasares", "annoyance"),
            new NamespacedKey("bigcasares", "golem"), () -> 0.0, () -> 0L, ignored -> { });
        for (int i = 0; i < 100; i++) {
            UUID id = UUID.randomUUID();
            Villager villager = (Villager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Villager.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "isValid" -> true;
                    case "isInvulnerable" -> { visits.incrementAndGet(); yield true; }
                    default -> throw new AssertionError("Unexpected call: " + method.getName());
                });
            runtime.entityAdded(villager);
        }
        runtime.check(List.of());
        runtime.tickChecks();
        assertEquals(32, visits.get());
        runtime.tickChecks();
        assertEquals(64, visits.get());
        runtime.shutdown();
    }
    @Test
    void slowPlayerScanDoesNotDelayTheNextMoodPass() {
        AtomicInteger visits = new AtomicInteger();
        World world = (World) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {World.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "isChunkLoaded" -> false;
                default -> null;
            });
        Location location = new Location(world, 0, 64, 0);
        UUID playerId = UUID.randomUUID();
        Player player = (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> playerId;
                case "isOnline", "isValid" -> true;
                case "getWorld" -> world;
                case "getLocation" -> location;
                case "getGameMode" -> GameMode.SURVIVAL;
                default -> null;
            });
        VillagerMoodRuntime runtime = new VillagerMoodRuntime(VillagerMoodSettings.defaults(),
            new NamespacedKey("bigcasares", "confined"), new NamespacedKey("bigcasares", "annoyance"),
            new NamespacedKey("bigcasares", "golem"), () -> 0.0, () -> 0L, ignored -> { });
        for (int i = 0; i < 100; i++) {
            UUID id = UUID.randomUUID();
            Villager villager = (Villager) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Villager.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "isValid" -> true;
                    case "isInvulnerable" -> { visits.incrementAndGet(); yield true; }
                    default -> throw new AssertionError("Unexpected call: " + method.getName());
                });
            runtime.entityAdded(villager);
        }
        runtime.check(List.of(player));
        for (int i = 0; i < 4; i++) runtime.tickChecks();
        assertEquals(100, visits.get());
        runtime.check(List.of(player));
        runtime.tickChecks();
        assertEquals(132, visits.get(), "a pending player scan must not starve mood updates");
        runtime.shutdown();
    }
}
