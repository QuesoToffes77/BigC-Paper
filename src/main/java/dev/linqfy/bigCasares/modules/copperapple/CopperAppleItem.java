package dev.linqfy.bigCasares.modules.copperapple;

import dev.linqfy.bigCasares.items.CustomItem;
import dev.linqfy.bigCasares.items.ModelDataUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;

public final class CopperAppleItem implements CustomItem {

    public static final String ID = "copper_apple";
    public static final int MODEL_DATA = 1001;

    private final NamespacedKey itemKey;
    private final ItemStack prototype;

    public CopperAppleItem(NamespacedKey itemKey) {
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
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.ABSORPTION,
            CopperAppleBalance.ABSORPTION_TICKS,
            CopperAppleBalance.ABSORPTION_AMPLIFIER,
            false,
            false,
            false
        ));
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.REGENERATION,
            CopperAppleBalance.REGENERATION_TICKS,
            CopperAppleBalance.REGENERATION_AMPLIFIER,
            false,
            false,
            false
        ));
    }

    private ItemStack buildPrototype() {
        ItemStack item = new ItemStack(Material.APPLE);
        ItemMeta meta = item.getItemMeta();


        // Try to set a translatable Component display name (Adventure) via reflection.
        // If Adventure is not available at compile-time/runtime, fall back to a plain name.
        try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method translatable = componentClass.getMethod("translatable", String.class);
            Object comp = translatable.invoke(null, "item.bigcasares.copper_apple");
            Method setDisplayName = meta.getClass().getMethod("setDisplayName", componentClass);
            setDisplayName.invoke(meta, comp);
        } catch (Throwable ignored) {
            // Adventure not present or reflection failed; use plain display name.
            meta.setDisplayName("Manzana de Cobre");
        }

        meta.setItemModel(itemKey);
        ModelDataUtil.writeCustomModelData(meta, MODEL_DATA);
        FoodComponent food = meta.getFood();
        food.setNutrition(CopperAppleBalance.NUTRITION);
        food.setSaturation(CopperAppleBalance.SATURATION);
        food.setCanAlwaysEat(true);
        meta.setFood(food);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }
}
