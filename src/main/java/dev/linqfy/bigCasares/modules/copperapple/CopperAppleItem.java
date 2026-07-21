package dev.linqfy.bigCasares.modules.copperapple;

import dev.linqfy.bigCasares.items.CatalogBackedCustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class CopperAppleItem extends CatalogBackedCustomItem {

    public static final String ID = "copper_apple";
    public static final int MODEL_DATA = 1001;

    public CopperAppleItem(CustomItemRegistry registry, NamespacedKey legacyItemKey) {
        super(registry, ID, MODEL_DATA, legacyItemKey);
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.INSTANT_HEALTH,
            1,
            0,
            false,
            false,
            false
        ));
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
}
