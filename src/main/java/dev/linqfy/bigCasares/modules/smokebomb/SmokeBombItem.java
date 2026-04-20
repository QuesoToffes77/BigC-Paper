package dev.linqfy.bigCasares.modules.smokebomb;

import dev.linqfy.bigCasares.items.CustomItem;
import dev.linqfy.bigCasares.items.ModelDataUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;

public final class SmokeBombItem implements CustomItem {

    public static final String ID = "smoke_bomb";
    public static final int MODEL_DATA = 1002;

    private final NamespacedKey itemKey;
    private final ItemStack prototype;

    public SmokeBombItem(NamespacedKey itemKey) {
        this.itemKey = itemKey;
        this.prototype = buildPrototype();
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int getCustomModelData() {
        return MODEL_DATA;
    }

    @Override
    public NamespacedKey getItemKey() {
        return itemKey;
    }

    @Override
    public ItemStack createItemStack(int amount) {
        int clampedAmount = Math.max(1, Math.min(amount, 16));
        ItemStack stack = prototype.clone();
        stack.setAmount(clampedAmount);
        return stack;
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
        // Smoke bombs are thrown, not consumed as food.
    }

    private ItemStack buildPrototype() {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();

        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method translatable = componentClass.getMethod("translatable", String.class);
            Object comp = translatable.invoke(null, "item.bigcasares.smoke_bomb");
            Method setDisplayName = meta.getClass().getMethod("setDisplayName", componentClass);
            setDisplayName.invoke(meta, comp);
        } catch (Throwable ignored) {
            meta.setDisplayName("Bomba de Humo");
        }

        meta.setItemModel(itemKey);
        ModelDataUtil.writeCustomModelData(meta, MODEL_DATA);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}
