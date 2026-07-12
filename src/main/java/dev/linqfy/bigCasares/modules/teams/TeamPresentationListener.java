package dev.linqfy.bigCasares.modules.teams;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TeamPresentationListener implements Listener {
    private final TeamPresentationService presentationService;

    public TeamPresentationListener(TeamPresentationService presentationService) {
        this.presentationService = presentationService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        presentationService.refreshPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        presentationService.clearPlayer(event.getPlayer().getUniqueId());
    }
}
