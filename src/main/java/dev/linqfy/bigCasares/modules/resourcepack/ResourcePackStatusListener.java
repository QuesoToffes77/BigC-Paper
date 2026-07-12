package dev.linqfy.bigCasares.modules.resourcepack;

import dev.linqfy.bigCasares.BigCasares;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

public final class ResourcePackStatusListener implements Listener {
    private final BigCasares plugin;
    private final ResourcePackService service;
    private final ResourcePackStatusHandler statusHandler;

    public ResourcePackStatusListener(BigCasares plugin, ResourcePackService service) {
        this.plugin = plugin;
        this.service = service;
        this.statusHandler = new ResourcePackStatusHandler(service);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        java.util.UUID playerId = player.getUniqueId();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            service.requestFor(playerId);

            if (service.isRequired()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    ResourcePackPlayerState state = service.state(playerId);
                    if (state == ResourcePackPlayerState.SENT || state == ResourcePackPlayerState.NOT_REQUESTED) {
                        player.kick(Component.text("Necesitas cargar el resource pack de BigCasares para jugar."));
                    }
                }, 400L); // 20 seconds timeout to accept the pack prompt
            }
        }, 20L); // 1 second delay to let client load
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        service.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        if (!service.isBigCasaresPack(event.getID())) {
            return;
        }
        statusHandler.handle(
            event.getID(),
            event.getPlayer().getUniqueId(),
            event.getStatus().name(),
            () -> event.getPlayer().kick(Component.text("Necesitas cargar el resource pack de BigCasares para jugar."))
        );
    }
}
