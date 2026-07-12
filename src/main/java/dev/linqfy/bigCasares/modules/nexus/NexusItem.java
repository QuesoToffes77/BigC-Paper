package dev.linqfy.bigCasares.modules.nexus;

import dev.linqfy.bigCasares.items.CustomItem;
import dev.linqfy.bigCasares.items.ModelDataUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;

public final class NexusItem implements CustomItem {

    public static final String ID = "nexus";
    public static final int MODEL_DATA = 1004;

    private final NamespacedKey itemKey;
    private final ItemStack prototype;

    public NexusItem(NamespacedKey itemKey) {
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
        int clampedAmount = Math.max(1, Math.min(amount, 64));
        ItemStack stack = prototype.clone();
        stack.setAmount(clampedAmount);
        return stack;
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
    }

    private ItemStack buildPrototype() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();

        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method translatable = componentClass.getMethod("translatable", String.class);
            Object comp = translatable.invoke(null, "item.bigcasares.nexus");
            Method setDisplayName = meta.getClass().getMethod("setDisplayName", componentClass);
            setDisplayName.invoke(meta, comp);
        } catch (Throwable ignored) {
            meta.setDisplayName("Nexus");
        }
        meta.setLore(java.util.List.of(
            "§7Un artefacto poderoso que protege tu base.",
            "§7Colócalo para establecer el corazón",
            "§7de tu equipo. Solo un Nexus por equipo.",
            "",
            "§eProtege cofres a su alrededor y",
            "§ete avisará si está bajo ataque."
        ));

        meta.setItemModel(itemKey);
        ModelDataUtil.writeCustomModelData(meta, MODEL_DATA);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}
