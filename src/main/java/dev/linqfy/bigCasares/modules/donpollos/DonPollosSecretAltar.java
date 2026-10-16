package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Lo que pasa con el inventario en el menu del secreto del Gordito (sin Paper, asi se prueba con un inventario
 * falso): poner una de las cosas que pide saca la cantidad justa aunque este repartida en varios stacks, sacarla la
 * devuelve, y si no alcanza no se toca nada. Lo que no entra en el inventario va a {@code overflow} (se tira al piso).
 */
final class DonPollosSecretAltar {

    /** Como se reconoce y como se arma cada cosa que pide (los items de Don Pollos los resuelve DonPollosItems). */
    interface Goods {
        boolean matches(DonPollosSecretOffering offering, ItemStack stack);

        ItemStack stack(DonPollosSecretOffering offering);
    }

    enum Result { PLACED, RETURNED, MISSING }

    private final Goods goods;

    DonPollosSecretAltar(Goods goods) {
        this.goods = goods;
    }

    /** Cuanto de esto tiene el jugador en el inventario. */
    int count(PlayerInventory inventory, DonPollosSecretOffering offering) {
        int have = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (goods.matches(offering, stack)) {
                have += stack.getAmount();
            }
        }
        return have;
    }

    /** Pone lo pedido (si alcanza) o, si ya estaba puesto, lo devuelve. */
    Result toggle(PlayerInventory inventory, Set<DonPollosSecretOffering> placed, DonPollosSecretOffering offering,
                  Consumer<ItemStack> overflow) {
        if (placed.remove(offering)) {
            give(inventory, goods.stack(offering), overflow);
            return Result.RETURNED;
        }
        if (count(inventory, offering) < offering.amount) {
            return Result.MISSING;
        }
        ItemStack[] contents = inventory.getStorageContents();
        int missing = offering.amount;
        for (int slot = 0; slot < contents.length && missing > 0; slot++) {
            ItemStack stack = contents[slot];
            if (goods.matches(offering, stack)) {
                int take = Math.min(missing, stack.getAmount());
                stack.setAmount(stack.getAmount() - take);
                inventory.setItem(slot, stack.getAmount() <= 0 ? null : stack);
                missing -= take;
            }
        }
        placed.add(offering);
        return Result.PLACED;
    }

    /** Devuelve todo lo puesto (al cerrar el menu sin terminar). */
    void returnAll(PlayerInventory inventory, Set<DonPollosSecretOffering> placed, Consumer<ItemStack> overflow) {
        for (DonPollosSecretOffering offering : placed) {
            give(inventory, goods.stack(offering), overflow);
        }
        placed.clear();
    }

    private static void give(PlayerInventory inventory, ItemStack stack, Consumer<ItemStack> overflow) {
        inventory.addItem(stack).values().forEach(overflow);
    }
}
