package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RocketJumpAirController {
    private static final int LANDING_GRACE_TICKS = 5;
    private static final int MAX_AIR_CONTROL_TICKS = 400;

    private final BigCasares plugin;
    private final Map<UUID, Integer> activeTicks = new HashMap<>();

    public RocketJumpAirController(BigCasares plugin, BukkitRuntimeRegistrations registrations) {
        this.plugin = plugin;
        registrations.scheduleRepeating("rocket-jump-air-control", this::heartbeat, 1L, 1L);
    }

    public void activate(Player player) {
        activeTicks.put(player.getUniqueId(), 0);
    }

    private void heartbeat() {
        Iterator<Map.Entry<UUID, Integer>> iterator = activeTicks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            Player player = plugin.getServer().getPlayer(entry.getKey());
            int ticks = entry.getValue() + 1;
            if (player == null || !player.isOnline() || player.isDead()
                || ticks >= MAX_AIR_CONTROL_TICKS
                || (ticks > LANDING_GRACE_TICKS && player.isOnGround())) {
                iterator.remove();
                continue;
            }

            Input input = player.getCurrentInput();
            if (!input.isForward() && !input.isBackward() && !input.isLeft() && !input.isRight()) {
                entry.setValue(ticks);
                continue;
            }
            Vector velocity = player.getVelocity();
            RocketJumpAirControl.Motion horizontal = RocketJumpAirControl.apply(
                velocity.getX(), velocity.getZ(), player.getLocation().getYaw(),
                input.isForward(), input.isBackward(), input.isLeft(), input.isRight()
            );
            velocity.setX(horizontal.x());
            velocity.setZ(horizontal.z());
            player.setVelocity(velocity);
            entry.setValue(ticks);
        }
    }
}
