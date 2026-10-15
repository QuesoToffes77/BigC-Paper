package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Entrega de items de la ruleta (pagos, liquidacion al salir y devolucion de apuestas), solo con API de Bukkit
 * para poder probarla. Lo que no entra se tira al piso y despues se aplican los limites de inventario del plugin
 * (el mismo enforcer que usa el shop), porque la entrega programatica no dispara pickup ni click.
 */
final class DonPollosRouletteDelivery {

    private DonPollosRouletteDelivery() {
    }

    static void give(Player player, ItemStack template, int amount, Consumer<Player> inventoryLimiter) {
        int max = Math.max(1, template.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = template.clone();
            stack.setAmount(Math.min(max, remaining));
            remaining -= stack.getAmount();
            player.getInventory().addItem(stack).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        }
        inventoryLimiter.accept(player);
    }
}
