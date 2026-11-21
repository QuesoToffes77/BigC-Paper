package dev.linqfy.bigCasares.modules.pveboss;

import dev.linqfy.bigCasares.modules.auth.AuthReadyEvent;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

public final class SahurRewardListener implements Listener {

    private final SahurRewardService rewards;
    private final BukkitRuntimeRegistrations registrations;

    public SahurRewardListener(SahurRewardService rewards, BukkitRuntimeRegistrations registrations) {
        this.rewards = Objects.requireNonNull(rewards, "rewards");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        registrations.scheduleImmediate("sahur-join-rewards", () -> rewards.deliverPending(playerId));
    }
    @EventHandler
    public void onAuthReady(AuthReadyEvent event) {
        var playerId = event.player().getUniqueId();
        registrations.scheduleImmediate("sahur-auth-rewards", () -> rewards.deliverPending(playerId));
    }
}
