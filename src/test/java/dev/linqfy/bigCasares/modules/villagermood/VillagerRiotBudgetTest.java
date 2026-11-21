package dev.linqfy.bigCasares.modules.villagermood;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataContainer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillagerRiotBudgetTest {
    @Test
    void largeRiotProcessesOnlyABoundedSliceEachPulse() throws Exception {
        AtomicInteger particles = new AtomicInteger();
        World world = proxy(World.class, (method, args) -> {
            if (method.equals("spawnParticle")) particles.incrementAndGet();
            return null;
        });
        Location playerLocation = new Location(world, 0, 64, 0);
        Player player = proxy(Player.class, (method, args) -> switch (method) {
            case "getUniqueId" -> new UUID(0, 1);
            case "isOnline", "isValid" -> true;
            case "isDead" -> false;
            case "getGameMode" -> GameMode.SURVIVAL;
            case "getWorld" -> world;
            case "getLocation" -> playerLocation;
            default -> null;
        });
        PersistentDataContainer pdc = proxy(PersistentDataContainer.class, (method, args) ->
            method.equals("getKeys") ? Set.of() : null);
        List<Villager> rioters = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            UUID id = UUID.randomUUID();
            Location at = new Location(world, 0, 64, 0);
            rioters.add(proxy(Villager.class, (method, args) -> switch (method) {
                case "isValid", "hasAI", "isAdult" -> true;
                case "isInvulnerable" -> false;
                case "getUniqueId" -> id;
                case "getWorld" -> world;
                case "getLocation", "getEyeLocation" -> at;
                case "getPersistentDataContainer" -> pdc;
                default -> null;
            }));
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("villager-mood.riot.summon-golem", false);
        AtomicLong clock = new AtomicLong();
        VillagerMoodRuntime runtime = new VillagerMoodRuntime(VillagerMoodSettings.load(yaml),
            new NamespacedKey("bigcasares", "confined"), new NamespacedKey("bigcasares", "annoyance"),
            new NamespacedKey("bigcasares", "golem"), () -> 1.0, clock::get, ignored -> { });
        var start = VillagerMoodRuntime.class.getDeclaredMethod("startRiot", Player.class, List.class);
        start.setAccessible(true);
        start.invoke(runtime, player, rioters);
        // Sounds require a live Bukkit registry; the budget applies equally after the opening sounds.
        var active = VillagerMoodRuntime.class.getDeclaredField("riots");
        active.setAccessible(true);
        Object riot = ((java.util.Map<?, ?>) active.get(runtime)).values().iterator().next();
        var sounds = riot.getClass().getDeclaredField("initialSoundsRemaining");
        sounds.setAccessible(true);
        sounds.setInt(riot, 0);
        runtime.tickActions();
        assertEquals(32, particles.get());
        clock.set(10);
        runtime.tickActions();
        assertEquals(64, particles.get());
        runtime.shutdown();
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
