package dev.linqfy.bigCasares.modules.shop;

import dev.linqfy.bigCasares.items.CustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.inventory.ItemStack;

public final class ShopItemResolver {

    private final CustomItemRegistry registry;

    public ShopItemResolver(CustomItemRegistry registry) {
        this.registry = registry;
    }

    public ItemStack createStack(ShopEntry entry) {
        if (entry.material() != null) {
            return new ItemStack(entry.material(), entry.amount());
        }

        CustomItem item = registry.findById(entry.customItemId())
            .orElseThrow(() -> new IllegalArgumentException("Custom item desconocido: " + entry.customItemId()));
        return item.createItemStack(entry.amount());
    }

    public boolean matches(ItemStack stack, ShopEntry entry) {
        if (stack == null || stack.getAmount() < 1) {
            return false;
        }
        if (entry.material() != null) {
            return stack.getType() == entry.material();
        }
        boolean directMatch;
        try {
            directMatch = registry.findByItemStack(stack)
                .map(item -> item.getId().equalsIgnoreCase(entry.customItemId()))
                .orElse(false);
        } catch (RuntimeException ex) {
            directMatch = false;
        }
        if (directMatch) {
            return true;
        }

        return registry.findById(entry.customItemId())
            .map(item -> item.createItemStack(1).getType() == stack.getType())
            .orElse(false);
    }
}
