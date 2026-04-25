package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

public final class CustomCrossbowDurabilityListener implements Listener {

    private final CustomCrossbowDurabilityService durabilityService;

    public CustomCrossbowDurabilityListener(CustomCrossbowDurabilityService durabilityService) {
        this.durabilityService = durabilityService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (event.getItem().getType() != Material.CROSSBOW && event.getItem().getType() != Material.BOW) {
            return;
        }

        int pendingCost = durabilityService.consumePending(event.getPlayer());
        if (pendingCost > 0) {
            event.setDamage(pendingCost);
        }
    }
}
