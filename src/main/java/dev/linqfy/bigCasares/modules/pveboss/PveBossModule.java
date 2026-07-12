package dev.linqfy.bigCasares.modules.pveboss;

import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.modules.model.JavaModelGateway;
import dev.linqfy.bigCasares.modules.model.JavaModelHandle;
import dev.linqfy.bigCasares.platform.ClientPlatform;
import dev.linqfy.bigCasares.platform.ClientPlatformGateway;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;

public final class PveBossModule implements PluginModule {

    public static final String MODULE_ID = "pve-boss-system";

    private final PveBossService service = new PveBossService();
    private final BigCasares plugin;
    private final ClientPlatformGateway platformGateway;
    private final Function<UUID, Boolean> resourcePackLoaded;
    private final JavaModelGateway javaModels;
    private PaperAbyssGuardianRuntime runtime;
    private boolean enabled;

    public PveBossModule() {
        this(null, ignored -> ClientPlatform.JAVA, ignored -> false);
    }

    public PveBossModule(
        BigCasares plugin,
        ClientPlatformGateway platformGateway,
        Function<UUID, Boolean> resourcePackLoaded
    ) {
        this(plugin, platformGateway, resourcePackLoaded, unavailableModels());
    }

    public PveBossModule(
        BigCasares plugin,
        ClientPlatformGateway platformGateway,
        Function<UUID, Boolean> resourcePackLoaded,
        JavaModelGateway javaModels
    ) {
        this.plugin = plugin;
        this.platformGateway = platformGateway;
        this.resourcePackLoaded = resourcePackLoaded;
        this.javaModels = java.util.Objects.requireNonNull(javaModels, "javaModels");
    }

    private static JavaModelGateway unavailableModels() {
        return new JavaModelGateway() {
            @Override
            public JavaModelHandle attach(org.bukkit.entity.Entity anchor, String modelKey) {
                throw new IllegalStateException("Java model gateway is required for live boss rendering");
            }

            @Override
            public boolean animate(JavaModelHandle handle, String animationKey) {
                throw new IllegalStateException("Java model gateway is required for live boss rendering");
            }

            @Override
            public void close(JavaModelHandle handle) {
                throw new IllegalStateException("Java model gateway is required for live boss rendering");
            }
        };
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public void onEnable() {
        enabled = true;
        if (plugin == null) {
            return;
        }
        plugin.saveResource("bosses/abyss-guardian.yml", false);
        File file = new File(plugin.getDataFolder(), "bosses/abyss-guardian.yml");
        AbyssGuardianDefinition definition = new AbyssGuardianDefinitionLoader()
            .load(YamlConfiguration.loadConfiguration(file));
        runtime = new PaperAbyssGuardianRuntime(
            plugin, definition, service, platformGateway, resourcePackLoaded, javaModels);
        runtime.start();
        plugin.getLogger().info("PvE Boss System listo: Guardián del Abismo cargado.");
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.stop();
            runtime = null;
        }
        service.shutdown(Instant.now());
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public PveBossService service() {
        return service;
    }

    public UUID spawnAbyssGuardian(Location location) {
        if (!enabled || runtime == null) {
            throw new IllegalStateException("PvE Boss System no está disponible.");
        }
        return runtime.spawn(location);
    }

    public int activeBossCount() {
        return runtime == null ? 0 : runtime.activeCount();
    }
}
