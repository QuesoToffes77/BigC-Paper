package dev.linqfy.bigCasares.modules.smokebomb;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class SmokeBombVisibilityListener implements Listener {

    private final SmokeConcealmentService concealmentService;

    public SmokeBombVisibilityListener(SmokeConcealmentService concealmentService) {
        this.concealmentService = concealmentService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        concealmentService.hideFromViewer(event.getPlayer());
    }
}
