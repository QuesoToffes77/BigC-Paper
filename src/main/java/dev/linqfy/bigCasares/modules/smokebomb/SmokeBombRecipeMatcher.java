package dev.linqfy.bigCasares.modules.smokebomb;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class SmokeBombRecipeMatcher {

    private SmokeBombRecipeMatcher() {
    }

    public static boolean matches(ItemStack[] matrix) {
        if (matrix.length != 9) {
            return false;
        }

        for (int i = 0; i < matrix.length; i++) {
            ItemStack slot = matrix[i];
            if (slot == null) {
                return false;
            }

            Material expected = switch (i) {
                case 0, 2, 6, 8 -> Material.GUNPOWDER;
                case 1, 3, 5, 7 -> Material.INK_SAC;
                case 4 -> Material.REDSTONE;
                default -> null;
            };

            if (slot.getType() != expected) {
                return false;
            }
        }

        return true;
    }
}
