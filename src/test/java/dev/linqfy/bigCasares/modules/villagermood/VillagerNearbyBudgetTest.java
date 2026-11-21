package dev.linqfy.bigCasares.modules.villagermood;

import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataContainer;
import java.util.Set;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillagerNearbyBudgetTest {
    @Test
    void movingPlayerChecksOnlyOneUnloadedChunkPerTickWithoutLoadingIt() {
        AtomicInteger checks = new AtomicInteger();
        World world = proxy(World.class, (method, args) -> switch (method) {
            case "isChunkLoaded" -> { checks.incrementAndGet(); yield false; }
            case "getChunkAt" -> throw new AssertionError("must not load a chunk");
            default -> null;
        });
        Location location = new Location(world, 0, 64, 0);
        Player player = player(world, location);
        VillagerMoodRuntime runtime = runtime();
        runtime.check(List.of(player));
        runtime.tickChecks();
        assertEquals(1, checks.get());
        location.setX(20);
        runtime.tickChecks();
        assertEquals(2, checks.get(), "normal movement should not abandon the scan");
        runtime.shutdown();
    }

    @Test
    void oversizedChunkIsProcessedInSlicesBeforeTakingAnotherSnapshot() {
        AtomicInteger visits = new AtomicInteger(), snapshots = new AtomicInteger();
        Entity[] entities = new Entity[100];
        for (int i = 0; i < entities.length; i++) {
            UUID id = UUID.randomUUID();
            entities[i] = proxy(Villager.class, (method, args) -> switch (method) {
                case "getUniqueId" -> { visits.incrementAndGet(); yield id; }
                default -> null;
            });
        }
        Chunk chunk = proxy(Chunk.class, (method, args) -> switch (method) {
            case "getEntities" -> { snapshots.incrementAndGet(); yield entities; }
            default -> null;
        });
        World world = proxy(World.class, (method, args) -> switch (method) {
            case "isChunkLoaded" -> true;
            case "getChunkAt" -> chunk;
            default -> null;
        });
        VillagerMoodRuntime runtime = runtime();
        runtime.check(List.of(player(world, new Location(world, 0, 64, 0))));
        runtime.tickChecks();
        assertEquals(32, visits.get());
        assertEquals(1, snapshots.get());
        runtime.tickChecks();
        assertEquals(64, visits.get());
        assertEquals(1, snapshots.get());
        runtime.shutdown();
    }

    @Test
    void disabledRiotAndMafiaDoNotScanPlayerChunks() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("villager-mood.riot.enabled", false);
        yaml.set("villager-mood.mafia.enabled", false);
        World world = proxy(World.class, (method, args) -> {
            if (method.equals("isChunkLoaded")) throw new AssertionError("disabled features must not scan");
            return null;
        });
        VillagerMoodRuntime runtime = new VillagerMoodRuntime(VillagerMoodSettings.load(yaml),
            new NamespacedKey("bigcasares", "confined"), new NamespacedKey("bigcasares", "annoyance"),
            new NamespacedKey("bigcasares", "golem"), () -> 0.0, () -> 0L, ignored -> { });
        runtime.check(List.of(player(world, new Location(world, 0, 64, 0))));
        runtime.tickChecks();
        runtime.shutdown();
    }

    @Test
    void bootstrapAndLoadedEntityCleanupAreSliced() {
        AtomicInteger bootstrapVisits = new AtomicInteger(), snapshots = new AtomicInteger();
        PersistentDataContainer pdc = proxy(PersistentDataContainer.class, (method, args) ->
            method.equals("getKeys") ? Set.of() : null);
        Entity[] entities = new Entity[100];
        for (int i = 0; i < entities.length; i++) {
            UUID id = UUID.randomUUID();
            entities[i] = proxy(Villager.class, (method, args) -> switch (method) {
                case "isValid" -> true;
                case "getUniqueId" -> { bootstrapVisits.incrementAndGet(); yield id; }
                case "getPersistentDataContainer" -> pdc;
                default -> null;
            });
        }
        Chunk chunk = proxy(Chunk.class, (method, args) -> switch (method) {
            case "isLoaded" -> true;
            case "getEntities" -> { snapshots.incrementAndGet(); yield entities; }
            default -> null;
        });
        VillagerMoodRuntime runtime = runtime();
        runtime.seedLoadedChunks(new Chunk[] {chunk});
        runtime.tickChecks();
        assertEquals(32, bootstrapVisits.get());
        assertEquals(1, snapshots.get());
        runtime.tickChecks();
        assertEquals(64, bootstrapVisits.get());
        assertEquals(1, snapshots.get());
        runtime.shutdown();

        AtomicInteger loadedVisits = new AtomicInteger();
        List<Entity> loaded = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            loaded.add(proxy(Entity.class, (method, args) -> switch (method) {
                case "isValid" -> { loadedVisits.incrementAndGet(); yield true; }
                case "getPersistentDataContainer" -> pdc;
                default -> null;
            }));
        }
        VillagerMoodRuntime second = runtime();
        second.entitiesLoaded(loaded);
        second.tickChecks();
        assertEquals(32, loadedVisits.get());
        second.tickChecks();
        assertEquals(64, loadedVisits.get());
        second.shutdown();
    }

    @Test
    void candidateIsRecheckedAgainstPlayersCurrentPosition() {
        AtomicInteger locationReads = new AtomicInteger(), moodReads = new AtomicInteger();
        World[] holder = new World[1];
        PersistentDataContainer pdc = proxy(PersistentDataContainer.class, (method, args) -> switch (method) {
            case "getKeys" -> Set.of();
            case "get" -> { moodReads.incrementAndGet(); yield null; }
            default -> null;
        });
        UUID villagerId = UUID.randomUUID();
        Villager villager = proxy(Villager.class, (method, args) -> switch (method) {
            case "getUniqueId" -> villagerId;
            case "isValid", "hasAI", "isAdult" -> true;
            case "isInvulnerable" -> false;
            case "getWorld" -> holder[0];
            case "getPersistentDataContainer" -> pdc;
            case "getLocation" -> { locationReads.incrementAndGet(); yield new Location(holder[0], 0, 64, 0); }
            default -> null;
        });
        Chunk chunk = proxy(Chunk.class, (method, args) -> method.equals("getEntities")
            ? new Entity[] {villager} : null);
        holder[0] = proxy(World.class, (method, args) -> switch (method) {
            case "isChunkLoaded" -> true;
            case "getChunkAt" -> chunk;
            default -> null;
        });
        Location playerAt = new Location(holder[0], 0, 64, 0);
        VillagerMoodRuntime runtime = runtime();
        runtime.check(List.of(player(holder[0], playerAt)));
        runtime.tickChecks();
        playerAt.setX(200);
        for (int i = 0; i < 30; i++) runtime.tickChecks();
        assertEquals(1, locationReads.get(), "candidate is deduplicated and revalidated");
        assertEquals(0, moodReads.get(), "out-of-range villager cannot trigger a riot or mafia");
        runtime.shutdown();
    }

    private static VillagerMoodRuntime runtime() {
        return new VillagerMoodRuntime(VillagerMoodSettings.defaults(), new NamespacedKey("bigcasares", "confined"),
            new NamespacedKey("bigcasares", "annoyance"), new NamespacedKey("bigcasares", "golem"),
            () -> 0.0, () -> 0L, ignored -> { });
    }

    private static Player player(World world, Location at) {
        UUID id = UUID.randomUUID();
        return proxy(Player.class, (method, args) -> switch (method) {
            case "getUniqueId" -> id;
            case "isOnline", "isValid" -> true;
            case "isDead" -> false;
            case "getGameMode" -> GameMode.SURVIVAL;
            case "getWorld" -> world;
            case "getLocation" -> at;
            default -> null;
        });
    }

    private interface Handler { Object call(String method, Object[] args); }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (obj, method, args) -> {
            if (method.getName().equals("hashCode")) return System.identityHashCode(obj);
            if (method.getName().equals("equals")) return obj == args[0];
            Object value = handler.call(method.getName(), args);
            if (value == null && method.getReturnType() == boolean.class) return false;
            return value;
        });
    }
}
