package dev.linqfy.bigCasares.modules.mobscaling;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

public class MobScalingListener implements Listener {

    private final JavaPlugin plugin;
    private final MobScalingSettings settings;
    private final NamespacedKey scaledKey;
    private final NamespacedKey customEntityKey;
    private final MobScalingMutations mutations;

    public MobScalingListener(JavaPlugin plugin, MobScalingSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.scaledKey = new NamespacedKey(plugin, "mob_scaled");
        this.customEntityKey = new NamespacedKey(plugin, "custom_entity");
        this.mutations = new MobScalingMutations(plugin);
    }

    public MobScalingMutations getMutations() {
        return mutations;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();

        if ("jeremy".equals(entity.getPersistentDataContainer().get(customEntityKey, PersistentDataType.STRING))) {
            return;
        }

        // Only scale Monsters and Trojan targets (Pig, Cow, etc.)
        boolean isMonster = entity instanceof Monster || entity instanceof Slime || entity instanceof Ghast || entity instanceof Phantom;
        boolean isTrojan = entity instanceof Pig || entity instanceof Cow || entity instanceof Squid || entity instanceof GlowSquid || entity instanceof Bat
                || entity instanceof Sheep || entity instanceof Chicken || entity instanceof Bee || entity instanceof Villager || entity instanceof Llama;
        
        if (!isMonster && !isTrojan) {
            return;
        }

        // Prevent double scaling
        if (entity.getPersistentDataContainer().has(scaledKey, PersistentDataType.BYTE)) {
            return;
        }
        entity.getPersistentDataContainer().set(scaledKey, PersistentDataType.BYTE, (byte) 1);

        long days = ChronoUnit.DAYS.between(settings.startDate(), LocalDate.now());
        if (days < 0) days = 0;

        // Apply health and damage multipliers
        double multiplier = settings.formulaA() * Math.pow(settings.formulaB(), days) + settings.formulaC();

        scaleAttribute(entity, Attribute.MAX_HEALTH, multiplier);
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            entity.setHealth(entity.getAttribute(Attribute.MAX_HEALTH).getValue());
        }
        
        scaleAttribute(entity, Attribute.ATTACK_DAMAGE, multiplier);

        // Check for mutation
        double chance = Math.min(settings.maxMutationChance(), settings.baseMutationChance() + (settings.mutationChancePerDay() * days));
        if (ThreadLocalRandom.current().nextDouble() <= chance) {
            mutations.applyRandomMutation(entity);
        }
    }

    private void scaleAttribute(LivingEntity entity, Attribute attribute, double multiplier) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            double base = instance.getBaseValue();
            instance.setBaseValue(base * multiplier);
        }
    }
}
