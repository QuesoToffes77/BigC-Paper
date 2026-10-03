package dev.linqfy.bigCasares.modules.glider;

import dev.linqfy.bigCasares.BigCasares;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Keeps the vanilla two-handed use pose active only for live Glider sessions. */
final class GliderGripPoseController implements AutoCloseable {

    private final BigCasares plugin;
    private final Map<UUID, EquipmentSlot> activeHands = new HashMap<>();

    GliderGripPoseController(BigCasares plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    void sync(Player player, GliderHandResolver.Selection selection) {
        EquipmentSlot hand = selection.hand();
        EquipmentSlot previous = activeHands.get(player.getUniqueId());
        if (previous != null && previous != hand) {
            clearActiveUse(player, previous);
        }

        ItemStack glider = itemIn(player, hand);
        ensureGripAnimation(glider);
        setItemIn(player, hand, glider);

        if (!player.hasActiveItem() || player.getActiveItemHand() != hand) {
            if (player.hasActiveItem()) {
                player.clearActiveItem();
            }
            player.startUsingItem(hand);
        }
        activeHands.put(player.getUniqueId(), hand);
    }

    void hide(UUID playerId) {
        EquipmentSlot hand = activeHands.remove(playerId);
        if (hand == null) {
            return;
        }
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null) {
            clearActiveUse(player, hand);
        }
    }

    int size() {
        return activeHands.size();
    }

    @Override
    public void close() {
        for (UUID playerId : java.util.List.copyOf(activeHands.keySet())) {
            hide(playerId);
        }
        activeHands.clear();
    }

    private static void ensureGripAnimation(ItemStack glider) {
        Consumable current = glider.getData(DataComponentTypes.CONSUMABLE);
        ItemUseAnimation animation = ItemUseAnimation.valueOf(GliderGripPosePolicy.animationName());
        if (current != null
            && current.animation() == animation
            && current.consumeSeconds() >= GliderGripPosePolicy.consumeSeconds()) {
            return;
        }
        glider.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
            .consumeSeconds(GliderGripPosePolicy.consumeSeconds())
            .animation(animation)
            .hasConsumeParticles(false));
    }

    private static ItemStack itemIn(Player player, EquipmentSlot hand) {
        return hand == EquipmentSlot.OFF_HAND
            ? player.getInventory().getItemInOffHand()
            : player.getInventory().getItemInMainHand();
    }

    private static void setItemIn(Player player, EquipmentSlot hand, ItemStack item) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItemInMainHand(item);
        }
    }

    private static void clearActiveUse(Player player, EquipmentSlot expectedHand) {
        if (player.hasActiveItem() && player.getActiveItemHand() == expectedHand) {
            player.clearActiveItem();
        }
    }
}
