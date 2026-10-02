package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

/**
 * Thin Bukkit mapping from {@link AcidRainMobType} to vanilla entity bases,
 * attributes. All three bases are vanilla hostile mobs. Their identity stays
 * in PDC rather than a Bukkit custom name so routine deaths are not broadcast
 * to the server console.
 */
final class BukkitAcidRainMobFactory {

    private BukkitAcidRainMobFactory() {
    }

    static EntityType entityTypeFor(AcidRainMobType type) {
        return switch (type) {
            case CRAWLER -> EntityType.SPIDER;
            case BRUTE -> EntityType.HUSK;
            case SPITTER -> EntityType.BOGGED;
        };
    }

    static void configure(LivingEntity entity, AcidRainMobType type) {
        switch (type) {
            case CRAWLER -> {
                setAttribute(entity, Attribute.MAX_HEALTH, 22.0);
                setAttribute(entity, Attribute.MOVEMENT_SPEED, 0.42);
            }
            case BRUTE -> {
                setAttribute(entity, Attribute.MAX_HEALTH, 60.0);
                setAttribute(entity, Attribute.ATTACK_DAMAGE, 8.0);
                setAttribute(entity, Attribute.MOVEMENT_SPEED, 0.16);
            }
            case SPITTER -> setAttribute(entity, Attribute.MAX_HEALTH, 20.0);
        }
        AttributeInstance health = entity.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            entity.setHealth(health.getValue());
        }
        entity.setCustomName(null);
        entity.setCustomNameVisible(false);
    }

    private static void setAttribute(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }
}
