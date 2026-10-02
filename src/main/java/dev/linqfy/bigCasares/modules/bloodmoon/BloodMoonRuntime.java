package dev.linqfy.bigCasares.modules.bloodmoon;

import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualProfile;
import dev.linqfy.bigCasares.modules.environment.EnvironmentalVisualService;
import dev.linqfy.bigCasares.modules.loot.DeathLootGuard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;

final class BloodMoonRuntime {
    private final Plugin plugin;
    private final BloodMoonSettings settings;
    private final BooleanSupplier otherEventActive;
    private final Random random = new Random();
    private final BukkitBloodMoonMobModifier modifier;
    private final BloodMoonSpawner spawner;
    private final EnvironmentalVisualService visuals;
    private final BloodMoonLootPolicy lootPolicy;
    private final DeathLootGuard lootGuard = new DeathLootGuard(4_096);
    private final Map<String, BloodMoonNightController> controllers = new LinkedHashMap<>();
    private final Map<String, BossBar> bossBars = new LinkedHashMap<>();
    private long tickCounter;

    BloodMoonRuntime(Plugin plugin, BloodMoonSettings settings, BooleanSupplier otherEventActive) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.otherEventActive = Objects.requireNonNull(otherEventActive, "otherEventActive");
        this.modifier = new BukkitBloodMoonMobModifier(plugin, settings.mobs());
        this.spawner = new BloodMoonSpawner(plugin, settings.spawning());
        this.visuals = new EnvironmentalVisualService(settings.visuals());
        this.lootPolicy = new BloodMoonLootPolicy(settings.loot());
        for (String worldName : settings.worlds()) {
            controllers.put(worldName, new BloodMoonNightController(settings.scheduling()));
        }
        reconcileLoadedEntities();
    }

    void tick() {
        tickCounter += 10L;
        if (!settings.enabled()) {
            return;
        }
        if (tickCounter % 20L == 0L) {
            tickLifecycle();
            updateBossBars();
        }
        if (settings.visuals().enabled() && tickCounter % settings.visuals().intervalTicks() == 0L) {
            showVisuals();
        }
        if (settings.spawning().multiplier() > 1.0
            && tickCounter % settings.spawning().intervalTicks() == 0L) {
            spawnExtras();
        }
    }

    BloodMoonActionResult start(World world) {
        if (world == null) {
            return BloodMoonActionResult.failure("Mundo no encontrado.");
        }
        if (!settings.enabled()) {
            return BloodMoonActionResult.failure("Blood Moon esta desactivada en la configuracion.");
        }
        if (!settings.affectsWorld(world.getName())) {
            return BloodMoonActionResult.failure("Ese mundo no esta habilitado para Blood Moon.");
        }
        if (!settings.allowWithOtherEvents() && safeOtherEventActive()) {
            return BloodMoonActionResult.failure("Hay otro evento ambiental activo.");
        }
        BloodMoonNightController controller = controller(world.getName());
        if (!controller.forceStart(world.getFullTime())) {
            return BloodMoonActionResult.failure(controller.active()
                ? "Blood Moon ya esta activa en ese mundo."
                : "Blood Moon solo puede comenzar durante la noche.");
        }
        begin(world, "manual");
        return BloodMoonActionResult.success("Blood Moon iniciada en " + world.getName() + ".");
    }

    BloodMoonActionResult stop(World world) {
        if (world == null) {
            return BloodMoonActionResult.failure("Mundo no encontrado.");
        }
        BloodMoonNightController controller = controllers.get(world.getName());
        if (controller == null || !controller.forceStop()) {
            return BloodMoonActionResult.failure("Blood Moon no esta activa en ese mundo.");
        }
        end(world, true, "manual");
        return BloodMoonActionResult.success("Blood Moon detenida en " + world.getName() + ".");
    }

    int stopAll() {
        int stopped = 0;
        for (String worldName : List.copyOf(activeWorlds())) {
            World world = plugin.getServer().getWorld(worldName);
            BloodMoonNightController controller = controllers.get(worldName);
            if (controller != null && controller.forceStop()) {
                if (world != null) {
                    end(world, true, "manual-all");
                }
                stopped++;
            }
        }
        return stopped;
    }

    boolean activeIn(World world) {
        if (world == null) {
            return false;
        }
        BloodMoonNightController controller = controllers.get(world.getName());
        return controller != null && controller.active();
    }

    Set<String> activeWorlds() {
        java.util.LinkedHashSet<String> worlds = new java.util.LinkedHashSet<>();
        controllers.forEach((world, controller) -> {
            if (controller.active()) {
                worlds.add(world);
            }
        });
        return Set.copyOf(worlds);
    }

    List<String> statusLines() {
        List<String> lines = new ArrayList<>();
        lines.add("§4§lBLOOD MOON");
        lines.add("§7Estado: " + (activeWorlds().isEmpty() ? "§aINACTIVE" : "§cACTIVE"));
        lines.add("§7Mundos activos: §f" + (activeWorlds().isEmpty() ? "ninguno" : String.join(", ", activeWorlds())));
        lines.add("§7Vida: §f" + String.format(Locale.ROOT, "%.2fx", settings.mobs().healthMultiplier()));
        lines.add("§7Velocidad: §f" + String.format(Locale.ROOT, "%.2fx", settings.mobs().movementSpeedMultiplier()));
        lines.add("§7Spawns: §f" + String.format(Locale.ROOT, "%.2fx", settings.spawning().multiplier()));
        return List.copyOf(lines);
    }

    void onSpawn(LivingEntity entity) {
        if (activeIn(entity.getWorld())) {
            applyModifier(entity);
        } else {
            modifier.remove(entity);
        }
        spawner.trackLoaded(entity);
    }

    void onEntitiesLoaded(Collection<? extends Entity> entities) {
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living) {
                onSpawn(living);
            }
        }
    }

    void onEntityRemoved(Entity entity) {
        spawner.release(entity);
    }

    void onWorldUnload(World world) {
        BloodMoonNightController controller = controllers.get(world.getName());
        if (controller != null && controller.forceStop()) {
            cleanupModifiers(world);
        }
        removeBossBar(world.getName());
        spawner.clearWorld(world.getName());
    }

    double damageMultiplier(LivingEntity attacker) {
        return activeIn(attacker == null ? null : attacker.getWorld()) && modifier.isEligible(attacker)
            ? settings.mobs().damageMultiplier()
            : 1.0;
    }

    void applyBonusLoot(EntityDeathEvent event) {
        LivingEntity entity = event == null ? null : event.getEntity();
        boolean eligible = entity != null
            && entity.getKiller() != null
            && activeIn(entity.getWorld())
            && modifier.isEligible(entity);
        if (!eligible || !lootGuard.markIfNew(entity.getUniqueId())) {
            return;
        }
        lootPolicy.roll(true).ifPresent(roll -> {
            if (roll.amount() > 0) {
                event.getDrops().add(new ItemStack(roll.material(), roll.amount()));
            }
            event.setDroppedExp(Math.min(10_000, event.getDroppedExp() + roll.experience()));
        });
    }

    void shutdown() {
        for (World world : plugin.getServer().getWorlds()) {
            cleanupModifiers(world);
        }
        controllers.values().forEach(BloodMoonNightController::forceStop);
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
        spawner.clear();
        lootGuard.clear();
    }

    private void tickLifecycle() {
        boolean conflict = !settings.allowWithOtherEvents() && safeOtherEventActive();
        for (String worldName : settings.worlds()) {
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                continue;
            }
            BloodMoonNightController controller = controller(worldName);
            if (controller.active() && conflict && controller.forceStop()) {
                end(world, true, "environment-conflict");
                continue;
            }
            BloodMoonTransition transition = controller.observe(world.getFullTime(), random.nextDouble(), conflict);
            if (transition == BloodMoonTransition.STARTED) {
                begin(world, "schedule");
            } else if (transition == BloodMoonTransition.STOPPED) {
                end(world, true, "sunrise");
            }
        }
    }

    private void begin(World world, String reason) {
        int modified = 0;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (applyModifier(entity)) {
                modified++;
            }
        }
        BossBar bossBar = Bukkit.createBossBar("☾ BLOOD MOON ☾", BarColor.RED, BarStyle.SOLID);
        bossBars.put(world.getName(), bossBar);
        for (Player player : world.getPlayers()) {
            bossBar.addPlayer(player);
            player.showTitle(Title.title(
                Component.text("☾ BLOOD MOON ☾"),
                Component.text("La noche se vuelve hostil..."),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofSeconds(1))));
        }
        playSound(world.getPlayers(), settings.sounds().startSound());
        plugin.getLogger().info("[BloodMoon] Started in " + world.getName()
            + " (" + reason + ", mobs modified: " + modified + ")");
    }

    private void end(World world, boolean announce, String reason) {
        int cleaned = cleanupModifiers(world);
        removeBossBar(world.getName());
        spawner.clearWorld(world.getName());
        if (announce) {
            for (Player player : world.getPlayers()) {
                player.sendMessage("§aLa Blood Moon ha terminado.");
            }
            playSound(world.getPlayers(), settings.sounds().stopSound());
        }
        plugin.getLogger().info("[BloodMoon] Ended in " + world.getName()
            + " (" + reason + ", mobs cleaned: " + cleaned + ")");
    }

    private boolean applyModifier(LivingEntity entity) {
        boolean changed = modifier.apply(entity);
        if (changed && settings.debug()) {
            plugin.getLogger().info("[BloodMoon] Modifiers applied to " + entity.getType()
                + " " + entity.getUniqueId());
        }
        return changed;
    }

    private int cleanupModifiers(World world) {
        int cleaned = 0;
        for (LivingEntity entity : world.getLivingEntities()) {
            if (modifier.remove(entity)) {
                cleaned++;
            }
        }
        return cleaned;
    }

    private void reconcileLoadedEntities() {
        for (World world : plugin.getServer().getWorlds()) {
            cleanupModifiers(world);
        }
    }

    private void showVisuals() {
        for (String worldName : activeWorlds()) {
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                continue;
            }
            for (Player player : world.getPlayers()) {
                visuals.show(player, EnvironmentalVisualProfile.BLOOD_MOON);
            }
        }
    }

    private void spawnExtras() {
        for (String worldName : activeWorlds()) {
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                continue;
            }
            int spawned = spawner.spawnCycle(world);
            if (settings.debug() && spawned > 0) {
                plugin.getLogger().info("[BloodMoon] Extra mobs spawned in " + worldName + ": " + spawned);
            }
        }
    }

    private void updateBossBars() {
        for (Map.Entry<String, BossBar> entry : bossBars.entrySet()) {
            World world = plugin.getServer().getWorld(entry.getKey());
            if (world == null || !activeIn(world)) {
                continue;
            }
            BossBar bar = entry.getValue();
            for (Player existing : List.copyOf(bar.getPlayers())) {
                if (existing.getWorld() != world || !existing.isOnline()) {
                    bar.removePlayer(existing);
                }
            }
            for (Player player : world.getPlayers()) {
                if (!bar.getPlayers().contains(player)) {
                    bar.addPlayer(player);
                }
            }
            long time = Math.floorMod(world.getFullTime(), 24_000L);
            double remaining = Math.max(0.0, Math.min(1.0, (23_000.0 - time) / 10_000.0));
            bar.setProgress(remaining);
            bar.setTitle("☾ BLOOD MOON ☾ | Amanecer: " + formatNightTicks(23_000L - time));
        }
    }

    private void removeBossBar(String worldName) {
        BossBar bar = bossBars.remove(worldName);
        if (bar != null) {
            bar.removeAll();
        }
    }

    private void playSound(Collection<? extends Player> players, String configured) {
        if (!settings.sounds().enabled()) {
            return;
        }
        Sound sound = resolveSound(configured);
        if (sound == null) {
            return;
        }
        for (Player player : players) {
            player.playSound(player.getLocation(), sound, settings.sounds().volume(), settings.sounds().pitch());
        }
    }

    private static Sound resolveSound(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String configured = raw.trim();
        NamespacedKey direct = NamespacedKey.fromString(configured.toLowerCase(Locale.ROOT));
        if (direct != null) {
            Sound sound = Registry.SOUNDS.get(direct);
            if (sound != null) {
                return sound;
            }
        }
        String legacy = configured.toUpperCase(Locale.ROOT);
        int namespace = legacy.indexOf(':');
        if (namespace >= 0) {
            legacy = legacy.substring(namespace + 1);
        }
        String expected = legacy;
        return Registry.SOUNDS.keyStream()
            .filter(key -> key.getKey().toUpperCase(Locale.ROOT).replace('.', '_').equals(expected))
            .findFirst()
            .map(Registry.SOUNDS::get)
            .orElse(null);
    }

    private BloodMoonNightController controller(String worldName) {
        return controllers.computeIfAbsent(worldName,
            ignored -> new BloodMoonNightController(settings.scheduling()));
    }

    private boolean safeOtherEventActive() {
        try {
            return otherEventActive.getAsBoolean();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String formatNightTicks(long remainingTicks) {
        long seconds = Math.max(0L, remainingTicks / 20L);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }
}

record BloodMoonActionResult(boolean success, String message) {
    static BloodMoonActionResult success(String message) {
        return new BloodMoonActionResult(true, message);
    }

    static BloodMoonActionResult failure(String message) {
        return new BloodMoonActionResult(false, message);
    }
}
