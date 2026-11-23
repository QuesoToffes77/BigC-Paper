package dev.linqfy.bigCasares.modules.toffy;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared equipment level library (0 to 200 by default). Other modules use it through
 * {@code BigCasares#getToffyModule()}, asked again each time because {@code /bigcasares reload} replaces the
 * instance.
 */
public final class ToffyModule implements PluginModule {
    public static final String MODULE_ID = "toffy-complementaries";
    public static final String CONFIG_FILE = ToffySettingsLoader.MAIN_FILE;
    public static final String VALUES_FILE = ToffySettingsLoader.VALUES_FILE;
    private static final String LOG_PREFIX = "[Toffy] ";
    private static final String MEMORY_FOLDER = "data/toffy-complementaries/players";

    private final BigCasares plugin;
    private final BukkitToffyEquipmentReader reader = new BukkitToffyEquipmentReader();
    private volatile ToffyService service;
    private volatile ToffySettings lastValidSettings;
    private ToffyMemoryTracker tracker;
    private RuntimeRegistrationScope compatibilityScope;

    public ToffyModule(BigCasares plugin) {
        this(plugin, null);
    }

    /**
     * @param previousSettings settings of the module instance being replaced by {@code /bigcasares reload}, used
     *                         when the file has become invalid so the level does not jump to the bundled defaults
     */
    public ToffyModule(BigCasares plugin, ToffySettings previousSettings) {
        this.plugin = plugin;
        this.lastValidSettings = previousSettings;
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        if (plugin == null) {
            return;
        }
        for (String name : List.of(CONFIG_FILE, VALUES_FILE)) {
            if (Files.notExists(dataPath(name))) {
                plugin.saveResource(name, false);
            }
        }
        ToffyConfigLoadResult loaded = reload();
        if (!loaded.valid() && lastValidSettings != null) {
            install(lastValidSettings);
            plugin.getLogger().severe(LOG_PREFIX + CONFIG_FILE + " / " + VALUES_FILE
                + " have errors; keeping the configuration that was working before the reload.");
        } else if (!loaded.valid()) {
            ToffyConfigLoadResult defaults = loadBundled();
            if (!defaults.valid()) {
                throw new IllegalStateException("Bundled " + CONFIG_FILE + " / " + VALUES_FILE + " are invalid: "
                    + String.join("; ", defaults.errors()));
            }
            install(defaults.settings());
            plugin.getLogger().severe(LOG_PREFIX + CONFIG_FILE + " / " + VALUES_FILE
                + " have errors; using the bundled defaults until they are fixed and reloaded.");
        }

        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        registrations.ownCleanup("toffy-service", () -> service = null);
        PluginCommand pluginCommand = plugin.getCommand("toffy");
        if (pluginCommand == null) {
            throw new IllegalStateException("Required command is not declared: toffy");
        }
        ToffyCommand command = new ToffyCommand(this);
        registrations.bindCommand("toffy-command", pluginCommand, command, command);

        ToffyMemoryTracker memoryTracker = new ToffyMemoryTracker(
            () -> new ToffyMemoryService(requireService().settings().memory()),
            new YamlToffyMemoryStorage(plugin.getDataFolder().toPath().resolve(MEMORY_FOLDER)),
            System::currentTimeMillis,
            warning -> plugin.getLogger().warning(LOG_PREFIX + warning));
        tracker = memoryTracker;
        registrations.ownCleanup("toffy-memory", () -> {
            memoryTracker.saveAll();
            tracker = null;
        });
        registrations.registerListener("toffy-memory-listener", new ToffyMemoryListener(this));
        long interval = requireService().settings().memory().checkIntervalTicks();
        registrations.scheduleRepeating("toffy-memory-check", this::checkOnlinePlayers, interval, interval);
        plugin.getLogger().info(LOG_PREFIX + "Module enabled (curve " + requireService().settings().curve().type()
            + ", maximum " + requireService().maximum() + ").");
    }

    @Override
    public void onDisable() {
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) {
            scope.close();
        }
        service = null;
    }

    /**
     * {@code true} once the module is enabled. When {@code modules.toffy-complementaries.enabled} is false every other
     * method throws {@link IllegalStateException}.
     */
    public boolean isAvailable() {
        return service != null;
    }

    /**
     * Level of {@code entity} rounded to {@code output.decimals}, between 0 and {@link #maximum()}. Must be called
     * on the main thread because it reads the entity's inventory.
     */
    public double level(LivingEntity entity) {
        return evaluate(entity).value();
    }

    /**
     * Full breakdown for {@code entity}. Must be called on the main thread.
     */
    public ToffyResult evaluate(LivingEntity entity) {
        ToffyService current = requireService();
        return current.evaluate(reader.read(entity, current));
    }

    /**
     * Breakdown for a loadout built by hand (for example a kit, a shop preview or a boss spawn table). Thread-safe.
     */
    public ToffyResult evaluate(ToffyLoadout loadout) {
        return requireService().evaluate(loadout);
    }

    /**
     * What the model sees on {@code entity}. Must be called on the main thread.
     */
    public ToffyLoadout readLoadout(LivingEntity entity) {
        return reader.read(entity, requireService());
    }

    /**
     * The real level: the current equipment plus what the player is remembered to own (the pieces, weapon and tool
     * it had before taking them off or swapping them for worse ones). Never below {@link #level(LivingEntity)}.
     * Mobs, and players while {@code memory.enabled} is false, get their current level. Main thread only.
     */
    public double realLevel(LivingEntity entity) {
        return evaluateReal(entity).value();
    }

    /**
     * Full breakdown of the real level. Also updates the player's memory with what it has now. Main thread only.
     */
    public ToffyResult evaluateReal(LivingEntity entity) {
        ToffyService current = requireService();
        ToffyLoadout loadout = reader.read(entity, current);
        ToffyMemoryTracker activeTracker = tracker;
        if (!(entity instanceof Player player) || activeTracker == null) {
            return current.evaluate(loadout);
        }
        return activeTracker.check(player.getUniqueId(), current, loadout).real();
    }

    /**
     * What {@code playerId} is remembered to own right now, read from its file if the player is not being checked.
     * Forgotten items are left out; empty while {@code memory.enabled} is false. Works for offline players.
     * Main thread only.
     */
    public ToffyMemory rememberedEquipment(UUID playerId) {
        requireService();
        ToffyMemoryTracker activeTracker = tracker;
        return activeTracker == null ? ToffyMemory.empty() : activeTracker.peek(playerId);
    }

    public double maximum() {
        return requireService().maximum();
    }

    /**
     * The current model, for item powers and settings. It is replaced on every reload, so do not keep it.
     */
    public ToffyService service() {
        return requireService();
    }

    /**
     * Last settings that loaded without errors. Kept after {@link #onDisable()} so the next instance can reuse them.
     */
    public ToffySettings lastValidSettings() {
        return lastValidSettings;
    }

    /**
     * Re-reads {@code toffy_complementaries.yml} and {@code toffy_valores.yml}. When they have errors the current model
     * is kept.
     */
    public synchronized ToffyConfigLoadResult reload() {
        ToffyConfigLoadResult result = loadInstalled();
        result.warnings().forEach(warning -> plugin.getLogger().warning(LOG_PREFIX + warning));
        result.errors().forEach(error -> plugin.getLogger().severe(LOG_PREFIX + error));
        if (result.valid()) {
            install(result.settings());
        } else if (service != null) {
            plugin.getLogger().severe(LOG_PREFIX + "Keeping the previous configuration.");
        }
        return result;
    }

    Server server() {
        return Objects.requireNonNull(plugin, "plugin").getServer();
    }

    void checkOnlinePlayers() {
        if (service == null || tracker == null) {
            return;
        }
        for (Player player : server().getOnlinePlayers()) {
            evaluateReal(player);
        }
    }

    void playerDied(Player player) {
        ToffyMemoryTracker activeTracker = tracker;
        if (service != null && activeTracker != null) {
            activeTracker.death(player.getUniqueId());
        }
    }

    void playerRespawned(Player player) {
        ToffyMemoryTracker activeTracker = tracker;
        if (activeTracker != null) {
            activeTracker.respawn(player.getUniqueId());
        }
    }

    void playerQuit(Player player) {
        ToffyMemoryTracker activeTracker = tracker;
        if (service != null && activeTracker != null) {
            evaluateReal(player);
            activeTracker.quit(player.getUniqueId());
        }
    }

    private void install(ToffySettings settings) {
        try {
            BukkitToffyKeyAudit.unknownKeys(settings)
                .forEach(warning -> plugin.getLogger().warning(LOG_PREFIX + warning));
        } catch (RuntimeException | LinkageError unavailableRegistry) {
            plugin.getLogger().warning(LOG_PREFIX + "Could not check keys against the server registries: "
                + unavailableRegistry);
        }
        service = new ToffyService(settings);
        lastValidSettings = settings;
    }

    private ToffyConfigLoadResult loadInstalled() {
        YamlConfiguration main = new YamlConfiguration();
        YamlConfiguration values = new YamlConfiguration();
        List<String> errors = new ArrayList<>();
        readInstalled(main, CONFIG_FILE, errors);
        readInstalled(values, VALUES_FILE, errors);
        if (!errors.isEmpty()) {
            return new ToffyConfigLoadResult(null, errors, List.of());
        }
        return ToffySettingsLoader.load(main, values);
    }

    private void readInstalled(YamlConfiguration yaml, String name, List<String> errors) {
        try {
            yaml.load(dataPath(name).toFile());
        } catch (IOException | InvalidConfigurationException invalid) {
            errors.add(name + ": cannot read the file: " + invalid.getMessage());
        }
    }

    private ToffyConfigLoadResult loadBundled() {
        try {
            return ToffySettingsLoader.load(bundled(CONFIG_FILE), bundled(VALUES_FILE));
        } catch (IOException invalid) {
            return new ToffyConfigLoadResult(null, List.of(invalid.getMessage()), List.of());
        }
    }

    private YamlConfiguration bundled(String name) throws IOException {
        InputStream stream = plugin.getResource(name);
        if (stream == null) {
            throw new IOException(name + " is missing from the jar");
        }
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        }
    }

    private Path dataPath(String name) {
        return plugin.getDataFolder().toPath().resolve(name);
    }

    private ToffyService requireService() {
        ToffyService current = service;
        if (current == null) {
            throw new IllegalStateException("Toffy Complementaries no esta disponible (modulo apagado o sin iniciar).");
        }
        return current;
    }
}
