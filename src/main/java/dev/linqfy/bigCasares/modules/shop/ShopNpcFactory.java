package dev.linqfy.bigCasares.modules.shop;

import dev.linqfy.bigCasares.platform.ClientEntityPresentationRegistry;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public final class ShopNpcFactory {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private final NamespacedKey shopIdKey;

    public ShopNpcFactory(JavaPlugin plugin) {
        this.shopIdKey = new NamespacedKey(plugin, "shop_npc_id");
    }

    public LivingEntity spawn(Location location, ShopNpcDefinition definition) {
        Objects.requireNonNull(location.getWorld(), "location world");
        LivingEntity entity = switch (definition.type()) {
            case PLAYER_MODEL -> spawnMannequin(location, definition);
            case VILLAGER -> spawnVillager(location, definition);
        };
        entity.getPersistentDataContainer().set(shopIdKey, PersistentDataType.STRING, definition.shopId());
        entity.customName(LEGACY.deserialize(definition.displayName()));
        entity.setCustomNameVisible(true);
        entity.setSilent(true);
        entity.setPersistent(true);
        entity.setInvulnerable(true);
        entity.setCollidable(false);
        applyEquipment(entity.getEquipment(), definition);
        return entity;
    }

    public Optional<String> shopId(org.bukkit.entity.Entity entity) {
        return Optional.ofNullable(entity.getPersistentDataContainer()
            .get(shopIdKey, PersistentDataType.STRING));
    }

    public boolean isShopNpc(org.bukkit.entity.Entity entity) {
        return shopId(entity).isPresent();
    }

    private Mannequin spawnMannequin(Location location, ShopNpcDefinition definition) {
        return location.getWorld().spawn(location, Mannequin.class, mannequin -> {
            ClientEntityPresentationRegistry.register(mannequin.getUniqueId(), "MANNEQUIN", "shop_npc_id");
            mannequin.setAI(false);
            mannequin.setGravity(false);
            mannequin.setImmovable(definition.immovable());
            mannequin.setDescription(LEGACY.deserialize("§7Tocá para abrir la tienda"));
            if (Mannequin.validPoses().contains(definition.pose())) {
                mannequin.setPose(definition.pose(), true);
            }
            com.destroystokyo.paper.profile.PlayerProfile profile = switch (definition.skinSource()) {
                case PLAYER_NAME -> Bukkit.createProfile(definition.skinValue());
                case PROFILE_UUID -> Bukkit.createProfile(java.util.UUID.fromString(definition.skinValue()));
            };
            mannequin.setProfile(ResolvableProfile.resolvableProfile(profile));
        });
    }

    private Villager spawnVillager(Location location, ShopNpcDefinition definition) {
        return location.getWorld().spawn(location, Villager.class, villager -> {
            villager.setAI(false);
            villager.setGravity(true);
            Villager.Profession profession = Registry.VILLAGER_PROFESSION.getOrThrow(
                NamespacedKey.minecraft(definition.profession()));
            Villager.Type type = Registry.VILLAGER_TYPE.getOrThrow(
                NamespacedKey.minecraft(definition.biomeType()));
            villager.setProfession(profession);
            villager.setVillagerType(type);
            villager.setVillagerLevel(definition.villagerLevel());
            if (definition.baby()) {
                villager.setBaby();
            } else {
                villager.setAdult();
            }
        });
    }

    private static void applyEquipment(EntityEquipment equipment, ShopNpcDefinition definition) {
        if (equipment == null) {
            return;
        }
        definition.equipment().forEach((slot, materialName) -> {
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                throw new IllegalArgumentException("Unknown shop NPC equipment material: " + materialName);
            }
            ItemStack item = new ItemStack(material);
            switch (slot.toLowerCase(Locale.ROOT).replace('_', '-')) {
                case "main-hand" -> equipment.setItemInMainHand(item);
                case "off-hand" -> equipment.setItemInOffHand(item);
                case "head", "helmet" -> equipment.setHelmet(item);
                case "chest", "chestplate" -> equipment.setChestplate(item);
                case "legs", "leggings" -> equipment.setLeggings(item);
                case "feet", "boots" -> equipment.setBoots(item);
                default -> throw new IllegalArgumentException("Unknown shop NPC equipment slot: " + slot);
            }
        });
    }
}
