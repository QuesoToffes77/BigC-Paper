package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;

public final class NativeCrossbowLoadListener implements Listener {

    private final CustomCrossbowData data;
    private final EchoArrowItem echoArrow;
    private final GoldenTippedAmethystArrowItem amethystArrow;
    private final CustomCrossbowSettings settings;
    private final BukkitRuntimeRegistrations registrations;

    public NativeCrossbowLoadListener(
        CustomCrossbowData data,
        EchoArrowItem echoArrow,
        GoldenTippedAmethystArrowItem amethystArrow,
        CustomCrossbowSettings settings,
        BukkitRuntimeRegistrations registrations
    ) {
        this.data = data;
        this.echoArrow = echoArrow;
        this.amethystArrow = amethystArrow;
        this.settings = settings;
        this.registrations = registrations;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLoad(EntityLoadCrossbowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack projectile = selectedProjectile(player);
        CustomCrossbowChargeType type = chargeType(projectile);
        if (type == null) {
            return;
        }
        int maximum = type == CustomCrossbowChargeType.ECHO_SHARD
            ? settings.maxEchoChargedCrossbows() : settings.maxAmethystChargedCrossbows();
        if (countLoaded(player, type) >= maximum) {
            event.setCancelled(true);
            player.sendMessage(type == CustomCrossbowChargeType.ECHO_SHARD
                ? "§cNo podés tener más de " + maximum + " ballestas cargadas con eco."
                : "§cNo podés tener más de " + maximum + " ballestas cargadas con amatista.");
            return;
        }
        ItemStack crossbow = event.getCrossbow();
        registrations.scheduleDelayed("native-crossbow-appearance", () -> {
            if (isLoadedWith(crossbow, type)) {
                data.applyNativeAppearance(crossbow, type);
            }
        }, 1L);
    }

    private ItemStack selectedProjectile(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isAmmunition(offhand)) {
            return offhand;
        }
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (isAmmunition(stack)) {
                return stack;
            }
        }
        return null;
    }

    private boolean isAmmunition(ItemStack stack) {
        return stack != null && switch (stack.getType()) {
            case ARROW, TIPPED_ARROW, SPECTRAL_ARROW, FIREWORK_ROCKET -> true;
            default -> false;
        };
    }

    private CustomCrossbowChargeType chargeType(ItemStack projectile) {
        if (echoArrow.matches(projectile)) {
            return CustomCrossbowChargeType.ECHO_SHARD;
        }
        if (amethystArrow.matches(projectile)) {
            return CustomCrossbowChargeType.AMETHYST_SHARD;
        }
        return null;
    }

    private int countLoaded(Player player, CustomCrossbowChargeType type) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isLoadedWith(stack, type)) {
                count++;
            }
        }
        return count;
    }

    private boolean isLoadedWith(ItemStack stack, CustomCrossbowChargeType type) {
        if (stack == null || stack.getType() != Material.CROSSBOW || !(stack.getItemMeta() instanceof CrossbowMeta meta)) {
            return false;
        }
        if (data.readCharge(stack).map(charge -> charge.type() == type).orElse(false)) {
            return true;
        }
        return meta.getChargedProjectiles().stream().anyMatch(projectile -> chargeType(projectile) == type);
    }
}
