package dev.linqfy.bigCasares.modules.airdrop;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public final class AirdropModule implements PluginModule {

    private static final String MODULE_ID = "airdrop-system";

    private static final long CHEST_LOCK_MILLIS = 5L * 60L * 1000L;
    /** Total landed lifetime, including the five-minute lock. */
    private static final long CHEST_LIFETIME_MILLIS = 40L * 60L * 1000L;

    private final BigCasares plugin;

    private AirdropService service;
    private YamlAirdropStorage storage;
    private AirdropListener listener;
    private AirdropAutoScheduler autoScheduler;
    private AirdropChestCountdown chestCountdown;
    private AirdropDefenderMobs defenderMobs;
    private BukkitTask fallingTask;
    private BukkitTask despawnTask;
    private BukkitRuntimeRegistrations registrations;
    private BooleanSupplier generationActive;
    private RuntimeRegistrationScope runtimeScope;
    private RuntimeRegistrationScope compatibilityScope;
    private boolean enabled;
    private long fallingTaskSequence;
    private String fallingTaskOwnershipId;

    public AirdropModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        this.compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        this.runtimeScope = scope;
        this.registrations = new BukkitRuntimeRegistrations(plugin, scope);
        this.generationActive = scope.generation()::isActive;
        AirdropSettings settings = AirdropSettings.fromConfig(plugin.getConfig());
        Map<String, Material> lootMaterials = new AirdropLootMaterialResolver().resolveAll();
        AirdropCustomItemValidator.validate(plugin.getCustomItemRegistry());
        AirdropEnchantmentValidator.validate();

        Path dataPath = plugin.getDataFolder().toPath()
                .resolve("data")
                .resolve("airdrop-system");

        YamlAirdropStorage ownedStorage = new YamlAirdropStorage(dataPath, plugin.getLogger());
        this.storage = ownedStorage;

        World world = Bukkit.getWorld("world");
        if (world == null) {
            throw new IllegalStateException("Default world 'world' not found");
        }

        BukkitAirdropWorldGateway gateway = new BukkitAirdropWorldGateway(world);
        service = new AirdropService(settings, gateway, ownedStorage);
        listener = new AirdropListener(service, lootMaterials, plugin.getCustomItemRegistry());
        listener.setClaimedCallback(this::onAirdropClaimed);
        defenderMobs = new AirdropDefenderMobs(plugin);
        scope.register("airdrop-defender-mobs", defenderMobs::close);

        scope.register("module-state", this::clearRuntimeState);
        registrations.registerListener("airdrop-listener", listener);
        registrations.registerListener("airdrop-defender-listener", defenderMobs);

        if (settings.automatic()) {
            autoScheduler = new AirdropAutoScheduler(
                System::currentTimeMillis,
                (delayTicks, action) -> registrations
                    .scheduleDelayed("airdrop-interval", action, delayTicks)::cancel,
                new StorageDeadlineStore(storage),
                settings.intervalMinutes() * 60_000L,
                () -> triggerAutomatic(world),
                delayTicks -> plugin.getLogger().info(
                    "[AirDrop] Next drop scheduled in " + (delayTicks * 50L / 1000L) + " seconds.")
            );
            plugin.getLogger().info("[AirDrop] Automatic system initialized. Interval: "
                + settings.intervalMinutes() + " minutes.");
            autoScheduler.start();
        } else {
            plugin.getLogger().info("[AirDrop] Automatic system disabled by config (automatic: false).");
        }

        registerCommand(world, registrations);
        enabled = true;
        restorePersistedDrop(world);

        plugin.getLogger().info("[AirdropModule] Enabled. Interval: " + settings.intervalMinutes() + " minutes.");
    }

    @Override
    public void onDisable() {
        if (!enabled && compatibilityScope == null) {
            return;
        }
        enabled = false;
        cancelIntervalTask();
        cancelFallingTask();
        cancelDespawnTask();
        stopChestCountdown();
        cleanupDefenders();
        if (listener != null) {
            listener.clearChestPosition();
        }
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
        plugin.getLogger().info("[AirdropModule] Disabled.");
    }

    public AirdropService getService() {
        return service;
    }

    private Optional<AirdropData> triggerAirdrop(World world) {
        Optional<AirdropData> result = service.spawnAirdrop();
        result.ifPresent(data -> {
            AirdropPosition pos = data.position();
            plugin.getServer().broadcastMessage(
                    "§6✈ §lAIRDROP §r" + data.type().color() + data.type().displayName()
                        + " §7[" + data.quality().color() + data.quality().displayName() + "§7]"
                        + " §7en camino en X: " + pos.x() + " Z: " + pos.z()
            );
            for (Player player : world.getPlayers()) {
                player.sendTitle("§6✈ §lAIRDROP", data.quality().color() + data.quality().displayName()
                    + " §7- " + data.type().displayName(), 10, 45, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 0.9f);
            }
            if (service.getSettings().quality().debug()) {
                plugin.getLogger().info("[AirDrop Debug] generated id=" + data.id()
                    + " type=" + data.type() + " quality=" + data.quality());
                for (AirdropReward reward : data.rewards()) {
                    plugin.getLogger().info("[AirDrop Debug] loot quality=" + reward.quality()
                        + " kind=" + reward.kind() + " item=" + reward.itemId()
                        + " amount=" + reward.amount());
                }
            }
            startFallingTask(world, pos);
        });
        return result;
    }

    private void registerCommand(World world, BukkitRuntimeRegistrations registrations) {
        var cmd = plugin.getCommand("airdrop");
        if (cmd == null) return;
        registrations.bindCommand("airdrop-command", cmd, (CommandSender sender, Command command, String label, String[] args) -> {
            if (args.length < 1 || !args[0].equalsIgnoreCase("spawn")) return false;
            if (!sender.hasPermission("bigcasares.airdrop.spawn")) {
                sender.sendMessage("§cNo tienes permiso.");
                return true;
            }
            Optional<AirdropData> result = triggerAirdrop(world);
            if (result.isPresent()) {
                sender.sendMessage("§aAirdrop forzado.");
            } else {
                sender.sendMessage("§cNo se pudo generar el airdrop.");
            }
            return true;
        }, null);
    }

    private void restorePersistedDrop(World world) {
        Optional<AirdropData> current = service.getCurrentDrop();
        if (current.isEmpty() || !current.get().isActive()) {
            return;
        }

        AirdropData data = current.get();
        if (data.phase() == AirdropPhase.FALLING) {
            startFallingTask(world, data.position());
            return;
        }

        if (data.phase() == AirdropPhase.LANDED) {
            AirdropPosition chestPosition = data.position();
            long now = System.currentTimeMillis();
            OptionalLong persistedUnlock = storage.loadUnlockAtMillis();
            long unlockAt;
            long despawnAt;
            if (persistedUnlock.isEmpty()) {
                // Migrate landed drops created by the old 15-minute/no-lock format.
                unlockAt = now + CHEST_LOCK_MILLIS;
                despawnAt = now + CHEST_LIFETIME_MILLIS;
                storage.saveUnlockAtMillis(unlockAt);
                storage.saveDespawnAtMillis(despawnAt);
            } else {
                unlockAt = persistedUnlock.getAsLong();
                despawnAt = storage.loadDespawnAtMillis()
                    .orElse(now + CHEST_LIFETIME_MILLIS);
            }
            if (despawnAt <= System.currentTimeMillis()) {
                // The chest expired while the server was offline: clean it up.
                world.getBlockAt(chestPosition.x(), chestPosition.y(), chestPosition.z()).setType(Material.AIR);
                listener.clearChestPosition();
                service.claim();
                removeTaggedDefenders(world);
                return;
            }
            world.getBlockAt(chestPosition.x(), chestPosition.y(), chestPosition.z()).setType(Material.CHEST);
            listener.setChestPosition(chestPosition, unlockAt);
            // Any defenders from before the restart are gone from memory but may
            // still exist in the world: clear the tag sweep, then guard anew.
            removeTaggedDefenders(world);
            spawnDefenders(world, data);
            startChestCountdown(world, chestPosition, unlockAt, despawnAt, data.type(), data.quality());
            scheduleDespawn(world, chestPosition, data.id(), despawnAt);
        }
    }

    private void startFallingTask(World world, AirdropPosition position) {
        cancelFallingTask();
        AirdropFallingTask task = new AirdropFallingTask(
            world,
            position,
            generationActive,
            landedPosition -> registrations.guard(() -> handleLanding(world, landedPosition)).run(),
            () -> registrations.guard(this::handleFallingAbort).run()
        );
        fallingTaskOwnershipId = "airdrop-falling-" + ++fallingTaskSequence;
        fallingTask = registrations.ownTask(
            fallingTaskOwnershipId,
            task.runTaskTimer(plugin, 0L, 1L)
        );
    }

    private void handleFallingAbort() {
        try {
            service.getCurrentDrop().ifPresent(data -> {
                service.markLanded(data.position());
                service.claim();
            });
            plugin.getLogger().warning("[AirDrop] Falling task aborted: invalid landing ground or obstructed space.");
        } finally {
            releaseFallingTaskOwnership();
        }
    }

    private void handleLanding(World world, AirdropPosition landedPosition) {
        try {
            Optional<AirdropData> landed = service.markLanded(landedPosition);
            if (landed.isPresent()) {
                long now = System.currentTimeMillis();
                long unlockAt = now + CHEST_LOCK_MILLIS;
                long despawnAt = now + CHEST_LIFETIME_MILLIS;
                storage.saveUnlockAtMillis(unlockAt);
                storage.saveDespawnAtMillis(despawnAt);
                listener.setChestPosition(landedPosition, unlockAt);
                spawnDefenders(world, landed.get());
                startChestCountdown(world, landedPosition, unlockAt, despawnAt,
                    landed.get().type(), landed.get().quality());
                announceLanding(world, landed.get());
                scheduleDespawn(world, landedPosition, landed.get().id(), despawnAt);
                return;
            }
            world.getBlockAt(landedPosition.x(), landedPosition.y(), landedPosition.z()).setType(Material.AIR);
        } finally {
            releaseFallingTaskOwnership();
        }
    }

    /**
     * Removes the chest when its 40-minute lifetime ends. Uses the persisted
     * deadline so the remaining time (and the chest countdown) survives a
     * server restart.
     */
    private void scheduleDespawn(World world, AirdropPosition position, UUID dropId, long despawnAtMillis) {
        cancelDespawnTask();
        long remainingTicks = Math.max(1L, (despawnAtMillis - System.currentTimeMillis() + 49L) / 50L);
        despawnTask = registrations.scheduleDelayed("airdrop-despawn-" + position.hashCode(), () -> {
            despawnTask = null;
            Optional<AirdropData> curr = service.getCurrentDrop();
            if (curr.isPresent() && curr.get().phase() == AirdropPhase.LANDED &&
                curr.get().id().equals(dropId) &&
                curr.get().position().equals(position)) {
                world.getBlockAt(position.x(), position.y(), position.z()).setType(Material.AIR);
                listener.clearChestPosition();
                service.claim();
                stopChestCountdown();
                cleanupDefenders();
                plugin.getServer().broadcastMessage("§e✈ El Airdrop ha desaparecido (tiempo agotado).");
            }
        }, remainingTicks);
    }

    private void cancelDespawnTask() {
        if (despawnTask != null) {
            if (!despawnTask.isCancelled()) {
                despawnTask.cancel();
            }
            despawnTask = null;
        }
    }

    /** Starts the floating countdown above the chest; any previous one is replaced. */
    private void startChestCountdown(
        World world,
        AirdropPosition position,
        long unlockAtMillis,
        long despawnAtMillis,
        AirdropType type,
        AirdropQuality quality
    ) {
        stopChestCountdown();
        chestCountdown = new AirdropChestCountdown(
            plugin, world, position, type, quality, unlockAtMillis, despawnAtMillis);
        chestCountdown.start();
    }

    private void announceLanding(World world, AirdropData data) {
        Location center = new Location(
            world, data.position().x() + 0.5, data.position().y() + 0.8, data.position().z() + 0.5
        );
        world.spawnParticle(Particle.END_ROD, center, 32, 1.2, 1.1, 1.2, 0.04);
        world.spawnParticle(Particle.CLOUD, center, 24, 0.8, 0.2, 0.8, 0.03);
        world.playSound(center, Sound.BLOCK_CHEST_OPEN, 1.25f, 0.75f);
        String message = "§a✦ " + data.type().color() + data.type().displayName()
            + " §7[" + data.quality().color() + data.quality().displayName() + "§7]"
            + " §7ha aterrizado. Buscá el cofre antes de que desaparezca.";
        if (service.getSettings().quality().profile(data.quality()).announceGlobally()) {
            plugin.getServer().broadcastMessage(message);
        } else {
            for (Player player : world.getPlayers()) {
                player.sendMessage(message);
            }
        }
    }

    /** Removes the floating countdown; idempotent. */
    private void stopChestCountdown() {
        if (chestCountdown != null) {
            chestCountdown.stop();
            chestCountdown = null;
        }
    }

    /** Called when a player claims the chest: drop its visuals and its guards. */
    private void onAirdropClaimed() {
        cancelDespawnTask();
        stopChestCountdown();
        cleanupDefenders();
    }

    /** Spawns the zombie defenders around a landed chest (no-op when disabled). */
    private void spawnDefenders(World world, AirdropData data) {
        if (defenderMobs == null || service == null) {
            return;
        }
        AirdropMobSettings mobs = service.getSettings().mobs();
        if (!mobs.enabled()) {
            cleanupDefenders();
            return;
        }
        AirdropQualityProfile profile = service.getSettings().quality().profile(data.quality());
        defenderMobs.spawnAround(world, data.position(), mobs, data.quality(), profile, data.id());
        if (service.getSettings().quality().debug()) {
            plugin.getLogger().info("[AirDrop Debug] type=" + data.type()
                + " quality=" + data.quality()
                + " guards=" + Math.min(mobs.maxTotal(), profile.guardCount())
                + " equipment=" + profile.equipmentLevel()
                + " loot-rolls=" + data.rewards().size());
        }
    }

    /** Removes every defender mob belonging to the current drop; idempotent. */
    private void cleanupDefenders() {
        if (defenderMobs != null) {
            defenderMobs.cleanup();
        }
    }

    /** Removes tagged defenders left over from a previous session. */
    private void removeTaggedDefenders(World world) {
        if (defenderMobs != null) {
            defenderMobs.removeTagged(world);
        }
    }

    private void cancelIntervalTask() {
        if (autoScheduler != null) {
            autoScheduler.stop();
        }
    }

    /**
     * Automatic cadence trigger: uses the exact same spawn path as the manual
     * {@code /airdrop spawn} command and logs the outcome once per interval.
     */
    private void triggerAutomatic(World world) {
        Optional<AirdropData> result = triggerAirdrop(world);
        if (result.isPresent()) {
            AirdropPosition pos = result.get().position();
            plugin.getLogger().info(
                "[AirDrop] Automatic drop triggered: spawned at X=" + pos.x() + " Z=" + pos.z());
        } else if (service != null && service.isActive()) {
            plugin.getLogger().info("[AirDrop] Automatic drop skipped: a drop is still active.");
        } else {
            plugin.getLogger().warning("[AirDrop] Automatic drop failed: no valid landing position found.");
        }
    }

    private void cancelFallingTask() {
        if (fallingTask != null) {
            if (!fallingTask.isCancelled()) {
                fallingTask.cancel();
            }
        }
        releaseFallingTaskOwnership();
    }

    private void releaseFallingTaskOwnership() {
        fallingTask = null;
        String ownershipId = fallingTaskOwnershipId;
        fallingTaskOwnershipId = null;
        if (runtimeScope != null && ownershipId != null) {
            runtimeScope.forget(ownershipId);
        }
    }

    private void clearRuntimeState() {
        enabled = false;
        cancelIntervalTask();
        cancelFallingTask();
        cancelDespawnTask();
        stopChestCountdown();
        cleanupDefenders();
        if (listener != null) {
            listener.clearChestPosition();
        }
        defenderMobs = null;
        listener = null;
        service = null;
        storage = null;
        registrations = null;
        generationActive = null;
        runtimeScope = null;
        autoScheduler = null;
    }

    private static final class StorageDeadlineStore implements AirdropAutoScheduler.DeadlineStore {

        private final YamlAirdropStorage storage;

        private StorageDeadlineStore(YamlAirdropStorage storage) {
            this.storage = storage;
        }

        @Override
        public OptionalLong nextDropAtMillis() {
            return storage.loadNextDropAtMillis();
        }

        @Override
        public void nextDropAtMillis(long epochMillis) {
            storage.saveNextDropAtMillis(epochMillis);
        }
    }
}
