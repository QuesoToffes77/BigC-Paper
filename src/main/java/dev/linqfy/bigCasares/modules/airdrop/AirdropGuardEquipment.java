package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

final class AirdropGuardEquipment {
    private AirdropGuardEquipment() {
    }

    static void equip(
        Zombie zombie,
        AirdropGuardLoadout loadout,
        double dropChance,
        NamespacedKey healthModifierKey,
        NamespacedKey damageModifierKey,
        AirdropQualityProfile profile
    ) {
        EntityEquipment equipment = zombie.getEquipment();
        if (equipment == null) {
            return;
        }
        equipment.setItemInMainHand(enchant(new ItemStack(loadout.weapon()), "sharpness", loadout.enchantmentLevel()));
        equipment.setHelmet(enchant(new ItemStack(loadout.helmet()), "protection", loadout.enchantmentLevel()));
        equipment.setChestplate(enchant(new ItemStack(loadout.chestplate()), "protection", loadout.enchantmentLevel()));
        equipment.setLeggings(enchant(new ItemStack(loadout.leggings()), "protection", loadout.enchantmentLevel()));
        equipment.setBoots(enchant(new ItemStack(loadout.boots()), "protection", loadout.enchantmentLevel()));
        setDropChances(equipment, dropChance);
        applyAttributes(zombie, profile, healthModifierKey, damageModifierKey);
    }

    static void applyAttributes(
        LivingEntity entity,
        AirdropQualityProfile profile,
        NamespacedKey healthModifierKey,
        NamespacedKey damageModifierKey
    ) {
        applyMultiplier(entity.getAttribute(Attribute.MAX_HEALTH), healthModifierKey, profile.healthMultiplier());
        applyMultiplier(entity.getAttribute(Attribute.ATTACK_DAMAGE), damageModifierKey, profile.damageMultiplier());
        AttributeInstance health = entity.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            entity.setHealth(health.getValue());
        }
    }

    static void setDropChances(EntityEquipment equipment, double chance) {
        float value = (float) Math.max(0.0, Math.min(1.0, chance));
        equipment.setItemInMainHandDropChance(value);
        equipment.setHelmetDropChance(value);
        equipment.setChestplateDropChance(value);
        equipment.setLeggingsDropChance(value);
        equipment.setBootsDropChance(value);
    }

    private static void applyMultiplier(AttributeInstance attribute, NamespacedKey key, double multiplier) {
        if (attribute == null || attribute.getModifier(key) != null || multiplier == 1.0) {
            return;
        }
        attribute.addModifier(new AttributeModifier(
            key, multiplier - 1.0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }

    private static ItemStack enchant(ItemStack item, String key, int level) {
        if (level <= 0) {
            return item;
        }
        var enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
        if (enchantment != null) {
            item.addEnchantment(enchantment, Math.min(level, enchantment.getMaxLevel()));
        }
        return item;
    }
}
