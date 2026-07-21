package dev.linqfy.bigCasares.modules.pveboss;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

public final class SahurRewardListener implements Listener {

    private final SahurRewardService rewards;

    public SahurRewardListener(SahurRewardService rewards) {
        this.rewards = Objects.requireNonNull(rewards, "rewards");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        rewards.deliverPending(event.getPlayer().getUniqueId());
    }
}
