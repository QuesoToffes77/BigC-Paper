package dev.linqfy.bigCasares.modules.specialitems;

import dev.linqfy.bigCasares.BigCasares;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

import java.util.Objects;
import java.util.function.LongSupplier;

public final class TrackerCompassRuntime implements Runnable {

    private final BigCasares plugin;
    private final TrackerCompassItem item;
    private final TrackerCompassService service;
    private final LongSupplier clock;

    public TrackerCompassRuntime(
        BigCasares plugin,
        TrackerCompassItem item,
        TrackerCompassService service
    ) {
        this(plugin, item, service, System::currentTimeMillis);
    }

    TrackerCompassRuntime(
        BigCasares plugin,
        TrackerCompassItem item,
        TrackerCompassService service,
        LongSupplier clock
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.item = Objects.requireNonNull(item, "item");
        this.service = Objects.requireNonNull(service, "service");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void run() {
        long now = clock.getAsLong();
        for (Player owner : plugin.getServer().getOnlinePlayers()) {
            TrackerCompassState state = service.state(owner.getUniqueId(), now);
            Player target = resolveTarget(owner, state, now);
            if (state != null && state.phase() == TrackerCompassState.Phase.TRACKING && target == null) {
                state = service.state(owner.getUniqueId(), now);
            }
            reconcileInventory(owner, state, target, now);
        }
    }

    private Player resolveTarget(Player owner, TrackerCompassState state, long now) {
        if (state == null || state.phase() != TrackerCompassState.Phase.TRACKING) {
            return null;
        }
        Player target = plugin.getServer().getPlayer(state.targetId());
        if (target == null || !target.isOnline() || target.isDead() || !owner.getWorld().equals(target.getWorld())) {
            service.invalidateTarget(state.targetId(), now);
            return null;
        }
        return target;
    }

    private void reconcileInventory(Player owner, TrackerCompassState state, Player target, long now) {
        ItemStack[] contents = owner.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack current = contents[slot];
            if (!item.matches(current)) {
                continue;
            }
            ItemStack replacement = item.createItemStack(current.getAmount());
            if (state != null) {
                applyPresentation(replacement, state, target, now);
            }
            owner.getInventory().setItem(slot, replacement);
        }
    }

    private static void applyPresentation(
        ItemStack stack,
        TrackerCompassState state,
        Player target,
        long now
    ) {
        if (!(stack.getItemMeta() instanceof CompassMeta meta)) {
            return;
        }
        TrackerCompassPresentation presentation;
        if (state.phase() == TrackerCompassState.Phase.TRACKING && target != null) {
            presentation = TrackerCompassPresentation.tracking(
                target.getName(), state.deadlineMillis() - now);
            meta.setLodestone(target.getLocation());
            meta.setLodestoneTracked(false);
        } else {
            presentation = TrackerCompassPresentation.cooldown(state.deadlineMillis() - now);
            meta.setLodestone(null);
        }
        meta.setDisplayName(presentation.displayName());
        meta.setLore(presentation.lore());
        meta.setEnchantmentGlintOverride(presentation.glint());
        stack.setItemMeta(meta);
    }
}
