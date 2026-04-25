package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CustomCrossbowDurabilityService {

    private final Map<UUID, PendingDurabilityCost> pendingCosts = new HashMap<>();

    public void record(Player player, int cost) {
        if (cost <= 0) {
            return;
        }
        long expiresAtTick = player.getWorld().getGameTime() + 5L;
        pendingCosts.merge(
            player.getUniqueId(),
            new PendingDurabilityCost(cost, expiresAtTick),
            (left, right) -> new PendingDurabilityCost(left.cost() + right.cost(), Math.max(left.expiresAtTick(), right.expiresAtTick()))
        );
    }

    public int consumePending(Player player) {
        PendingDurabilityCost pending = pendingCosts.remove(player.getUniqueId());
        if (pending == null || player.getWorld().getGameTime() > pending.expiresAtTick()) {
            return 0;
        }
        return pending.cost();
    }

    public void applyDirect(ItemStack itemStack, int cost) {
        if (itemStack == null || cost <= 0 || !itemStack.hasItemMeta()) {
            return;
        }
        if (!(itemStack.getItemMeta() instanceof Damageable meta)) {
            return;
        }

        int maxDurability = itemStack.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return;
        }

        int nextDamage = meta.getDamage() + cost;
        if (nextDamage >= maxDurability) {
            itemStack.setAmount(Math.max(0, itemStack.getAmount() - 1));
            return;
        }

        meta.setDamage(nextDamage);
        itemStack.setItemMeta(meta);
    }

    private record PendingDurabilityCost(int cost, long expiresAtTick) {
    }
}
