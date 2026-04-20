package dev.linqfy.bigCasares.modules.shop;

import org.bukkit.inventory.ItemStack;

public record ShopTransactionResult(
    boolean success,
    String message,
    ItemStack stack,
    int overflowRemoved
) {

    public static ShopTransactionResult success(String message, ItemStack stack) {
        return new ShopTransactionResult(true, message, stack, 0);
    }

    public static ShopTransactionResult success(String message, ItemStack stack, int overflowRemoved) {
        return new ShopTransactionResult(true, message, stack, overflowRemoved);
    }

    public static ShopTransactionResult failure(String message) {
        return new ShopTransactionResult(false, message, null, 0);
    }
}
