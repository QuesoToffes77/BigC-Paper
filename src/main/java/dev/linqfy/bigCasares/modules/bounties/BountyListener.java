package dev.linqfy.bigCasares.modules.bounties;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class BountyListener implements Listener {

    private final BountyService service;
    private final BountyEconomy economy;

    public BountyListener(BountyService service, BountyEconomy economy) {
        this.service = service;
        this.economy = economy;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }

        BountyProcessingResult result = service.handlePlayerKill(victim.getUniqueId(), killer.getUniqueId());

        if (result.paidExistingBounty()) {
            killer.sendMessage("§aCobraste " + economy.format(result.paidOutAmount()) + " por la bounty de " + victim.getName() + ".");
        }

        if (result.createdNewBounty()) {
            victim.sendMessage("§cTu muerte genero una nueva bounty de " + economy.format(result.newBountyAmount()) + ".");
            killer.sendMessage("§e" + victim.getName() + " ahora tiene una nueva bounty de " + economy.format(result.newBountyAmount()) + ".");
            return;
        }

        killer.sendMessage("§7" + victim.getName() + " no tenia balance suficiente para generar nueva bounty.");
    }
}
