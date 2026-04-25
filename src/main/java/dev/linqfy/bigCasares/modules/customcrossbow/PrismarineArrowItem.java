package dev.linqfy.bigCasares.modules.customcrossbow;

import dev.linqfy.bigCasares.items.CustomItem;
import dev.linqfy.bigCasares.items.ModelDataUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;

public final class PrismarineArrowItem implements CustomItem {

    public static final String ID = "prismarine_arrow";
    public static final int MODEL_DATA = 1003;

    private final NamespacedKey itemKey;
    private final ItemStack prototype;

    public PrismarineArrowItem(NamespacedKey itemKey) {
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
        ItemStack stack = prototype.clone();
        stack.setAmount(Math.max(1, Math.min(64, amount)));
        return stack;
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
        // Ammunition is consumed by vanilla bow/crossbow handling.
    }

    private ItemStack buildPrototype() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method translatable = componentClass.getMethod("translatable", String.class);
            Object comp = translatable.invoke(null, "item.bigcasares.prismarine_arrow");
            Method setDisplayName = meta.getClass().getMethod("setDisplayName", componentClass);
            setDisplayName.invoke(meta, comp);
        } catch (Throwable ignored) {
            meta.setDisplayName("Flecha de Prismarina");
        }

        ModelDataUtil.writeCustomModelData(meta, MODEL_DATA);
        meta.setItemModel(itemKey);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}
