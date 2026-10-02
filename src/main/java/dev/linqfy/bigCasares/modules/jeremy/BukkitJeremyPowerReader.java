package dev.linqfy.bigCasares.modules.jeremy;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

final class BukkitJeremyPowerReader {
    JeremyPlayerPower read(Player player) {
        int protection = 0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            protection += enchantmentLevel(armor, "protection");
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int offensive = 0;
        for (String key : new String[]{"sharpness", "smite", "bane_of_arthropods", "power", "impaling", "density", "breach"}) {
            offensive += enchantmentLevel(weapon, key);
        }
        PotionEffect resistance = player.getPotionEffect(PotionEffectType.RESISTANCE);
        int resistanceLevel = resistance == null ? 0 : resistance.getAmplifier() + 1;
        return new JeremyPlayerPower(
            attributeValue(player, Attribute.ARMOR),
            attributeValue(player, Attribute.ARMOR_TOUGHNESS),
            protection,
            attributeValue(player, Attribute.MAX_HEALTH),
            player.getAbsorptionAmount(),
            resistanceLevel,
            weaponDamage(weapon == null ? Material.AIR : weapon.getType()),
            offensive
        );
    }

    private static int enchantmentLevel(ItemStack item, String key) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        for (java.util.Map.Entry<Enchantment, Integer> enchantment : item.getEnchantments().entrySet()) {
            if (enchantment.getKey().getKey().getKey().equals(key)) {
                return enchantment.getValue();
            }
        }
        return 0;
    }

    private static double attributeValue(Player player, Attribute attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getValue();
    }

    private static double weaponDamage(Material material) {
        String name = material.name();
        if (name.endsWith("_SWORD")) {
            if (name.startsWith("NETHERITE")) return 8.0;
            if (name.startsWith("DIAMOND")) return 7.0;
            if (name.startsWith("IRON")) return 6.0;
            if (name.startsWith("STONE")) return 5.0;
            if (name.startsWith("GOLDEN")) return 4.0;
            return 4.0;
        }
        if (name.endsWith("_AXE")) {
            if (name.startsWith("NETHERITE") || name.startsWith("DIAMOND")) return 9.0;
            if (name.startsWith("IRON")) return 9.0;
            if (name.startsWith("STONE")) return 9.0;
            return 7.0;
        }
        return switch (material) {
            case TRIDENT -> 9.0;
            case MACE -> 6.0;
            case BOW, CROSSBOW -> 6.0;
            default -> 1.0;
        };
    }
}
