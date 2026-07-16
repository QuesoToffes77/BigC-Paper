package dev.linqfy.bigCasares.modules.airdrop;

import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public final class AirdropModule implements PluginModule {

    private static final String MODULE_ID = "airdrop-system";

    private final JavaPlugin plugin;

    private AirdropService service;
    private AirdropListener listener;
    private BukkitTask intervalTask;
    private BukkitTask fallingTask;
    private BukkitRuntimeRegistrations registrations;
    private BooleanSupplier generationActive;
    private RuntimeRegistrationScope runtimeScope;
    private RuntimeRegistrationScope compatibilityScope;
    private boolean enabled;
    private long fallingTaskSequence;
    private String fallingTaskOwnershipId;

    public AirdropModule(JavaPlugin plugin) {
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

        Path dataPath = plugin.getDataFolder().toPath()
                .resolve("data")
                .resolve("airdrop-system");

        YamlAirdropStorage storage = new YamlAirdropStorage(dataPath, plugin.getLogger());

        World world = Bukkit.getWorld("world");
        if (world == null) {
            throw new IllegalStateException("Default world 'world' not found");
        }

        BukkitAirdropWorldGateway gateway = new BukkitAirdropWorldGateway(world);
        service = new AirdropService(settings, gateway, storage);
        listener = new AirdropListener(service, lootMaterials);

        scope.register("module-state", this::clearRuntimeState);
        registrations.registerListener("airdrop-listener", listener);

        long intervalTicks = (long) settings.intervalMinutes() * 60L * 20L;
        intervalTask = registrations.scheduleRepeating(
            "airdrop-interval",
            () -> triggerAirdrop(world),
            intervalTicks,
            intervalTicks
        );

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
                    "Â§6âœˆ Airdrop en camino en X: " + pos.x() + " Z: " + pos.z()
            );
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
                sender.sendMessage("Â§cNo tienes permiso.");
                return true;
            }
            Optional<AirdropData> result = triggerAirdrop(world);
            if (result.isPresent()) {
                sender.sendMessage("Â§aAirdrop forzado.");
            } else {
                sender.sendMessage("Â§cNo se pudo generar el airdrop.");
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
            world.getBlockAt(chestPosition.x(), chestPosition.y(), chestPosition.z()).setType(Material.CHEST);
            listener.setChestPosition(chestPosition);
        }
    }

    private void startFallingTask(World world, AirdropPosition position) {
        cancelFallingTask();
        AirdropFallingTask task = new AirdropFallingTask(
            world,
            position,
            generationActive,
            landedPosition -> registrations.guard(() -> handleLanding(world, landedPosition)).run()
        );
        fallingTaskOwnershipId = "airdrop-falling-" + ++fallingTaskSequence;
        fallingTask = registrations.ownTask(
            fallingTaskOwnershipId,
            task.runTaskTimer(plugin, 0L, 1L)
        );
    }

    private void handleLanding(World world, AirdropPosition landedPosition) {
        try {
            Optional<AirdropData> landed = service.markLanded(landedPosition);
            if (landed.isPresent()) {
                listener.setChestPosition(landedPosition);
                return;
            }
            world.getBlockAt(landedPosition.x(), landedPosition.y(), landedPosition.z()).setType(Material.AIR);
        } finally {
            releaseFallingTaskOwnership();
        }
    }

    private void cancelIntervalTask() {
        if (intervalTask != null) {
            if (!intervalTask.isCancelled()) {
                intervalTask.cancel();
            }
            intervalTask = null;
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
        if (listener != null) {
            listener.clearChestPosition();
        }
        listener = null;
        service = null;
        registrations = null;
        generationActive = null;
        runtimeScope = null;
    }
}
