package dev.linqfy.bigCasares.modules.resourcepack;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.platform.ClientPlatform;
import dev.linqfy.bigCasares.platform.ClientPlatformGateway;
import org.bukkit.event.HandlerList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;

public final class ResourcePackModule implements PluginModule {
    private final BigCasares plugin;
    private final ClientPlatformGateway platformGateway;
    private ResourcePackService service;
    private ResourcePackStatusListener listener;

    public ResourcePackModule(BigCasares plugin) {
        this(plugin, ignored -> ClientPlatform.JAVA);
    }

    public ResourcePackModule(BigCasares plugin, ClientPlatformGateway platformGateway) {
        this.plugin = plugin;
        this.platformGateway = platformGateway == null ? ignored -> ClientPlatform.JAVA : platformGateway;
    }

    @Override
    public String getId() {
        return "resource-pack-system";
    }

    @Override
    public void onEnable() {
        if (plugin == null) {
            return;
        }
        Optional<ResourcePackManifest> manifest = loadManifest();
        if (manifest.isEmpty()) {
            plugin.getLogger().warning("No generated resource-pack manifest was found; automatic Java pack delivery is disabled.");
            return;
        }
        ResourcePackSettings settings = ResourcePackSettings.load(plugin.getConfig());
        this.service = new ResourcePackService(
            settings,
            manifest.get(),
            new BukkitResourcePackGateway(plugin.getServer()),
            platformGateway
        );
        this.listener = new ResourcePackStatusListener(plugin, service);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void onDisable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        service = null;
    }

    public Optional<ResourcePackService> service() {
        return Optional.ofNullable(service);
    }

    private Optional<ResourcePackManifest> loadManifest() {
        try (var resource = plugin.getResource("generated-resourcepacks/manifest.json")) {
            if (resource != null) {
                return Optional.of(ResourcePackManifest.fromJson(new String(resource.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)));
            }
        } catch (IOException | IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "Invalid bundled resource-pack manifest", ex);
        }

        Path developmentManifest = Path.of(System.getProperty("user.dir"), "build", "generated-resourcepacks", "manifest.json");
        if (Files.isRegularFile(developmentManifest)) {
            try {
                return Optional.of(ResourcePackManifest.fromJson(Files.readString(developmentManifest)));
            } catch (IOException | IllegalArgumentException ex) {
                plugin.getLogger().log(Level.WARNING, "Invalid generated resource-pack manifest", ex);
            }
        }
        return Optional.empty();
    }
}
