package dev.linqfy.bigCasares.modules.skillrating;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Locale;

public final class SkillRatingListener implements Listener {

    private final SkillRatingService service;

    public SkillRatingListener(SkillRatingService service) {
        this.service = service;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        SkillRatingUpdate update = service.recordKill(killer.getUniqueId(), victim.getUniqueId());
        killer.sendMessage("§aGanaste Skill Rating por matar a " + victim.getName() + ".");
        killer.sendMessage("§7" + SkillRatingView.format(killer.getName(), update.killerState(), service.getSettings().skillRatingScale()));

        if (update.killerTierChanged()) {
            killer.sendMessage("§eSubiste al Tier " + update.killerState().tier() + ".");
        }
    }

}
