package dev.linqfy.bigCasares.modules.cosmicniv;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.LinkedHashMap;
import java.util.Map;

final class BukkitCosmicNivEquipmentReader {
    private static final int HOTBAR_SIZE = 9;
    private static final NamespacedKey CATALOG_ID_KEY = new NamespacedKey("bigcasares", "item_id");

    private CosmicNivService keysFor;
    private Map<String, NamespacedKey> persistentKeys = Map.of();

    CosmicNivLoadout read(LivingEntity entity, CosmicNivService service) {
        if (entity == null) {
            return CosmicNivLoadout.empty();
        }
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return CosmicNivLoadout.empty();
        }
        Map<String, NamespacedKey> keys = persistentKeys(service);
        CosmicNivLoadout.Builder builder = CosmicNivLoadout.builder();
        armor(builder, service, keys, CosmicNivArmorSlot.HELMET, equipment.getHelmet());
        armor(builder, service, keys, CosmicNivArmorSlot.CHESTPLATE, equipment.getChestplate());
        armor(builder, service, keys, CosmicNivArmorSlot.LEGGINGS, equipment.getLeggings());
        armor(builder, service, keys, CosmicNivArmorSlot.BOOTS, equipment.getBoots());

        if (entity instanceof Player player) {
            playerHeld(builder, service, keys, player.getInventory(), service.settings().heldSearch());
        } else {
            held(builder, service, keys, equipment.getItemInMainHand());
            held(builder, service, keys, equipment.getItemInOffHand());
        }
        return builder.build();
    }

    private synchronized Map<String, NamespacedKey> persistentKeys(CosmicNivService service) {
        if (keysFor != service) {
            Map<String, NamespacedKey> keys = new LinkedHashMap<>();
            for (String rawKey : service.persistentDataKeys()) {
                NamespacedKey key = NamespacedKey.fromString(rawKey);
                if (key != null) {
                    keys.put(rawKey, key);
                }
            }
            persistentKeys = Map.copyOf(keys);
            keysFor = service;
        }
        return persistentKeys;
    }

    static CosmicNivItem toItem(ItemStack stack, CosmicNivService service, Map<String, NamespacedKey> persistentKeys) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
            return null;
        }
        Map<String, Integer> enchantments = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> enchantment : stack.getEnchantments().entrySet()) {
            enchantments.put(enchantment.getKey().getKey().toString(), enchantment.getValue());
        }
        String catalogId = null;
        String itemModel = null;
        Map<String, String> persistentData = new LinkedHashMap<>();
        if (!service.settings().customItems().isEmpty() && stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            PersistentDataContainer data = meta.getPersistentDataContainer();
            catalogId = readString(data, CATALOG_ID_KEY);
            if (meta.hasItemModel() && meta.getItemModel() != null) {
                itemModel = meta.getItemModel().toString();
            }
            persistentKeys.forEach((rawKey, key) -> {
                String value = readString(data, key);
                if (value != null) {
                    persistentData.put(rawKey, value);
                }
            });
        }
        return service.identify(new CosmicNivItemIdentity(stack.getType().getKey().toString(), catalogId, itemModel,
            persistentData, enchantments));
    }

    private static String readString(PersistentDataContainer data, NamespacedKey key) {
        return data.has(key, PersistentDataType.STRING) ? data.get(key, PersistentDataType.STRING) : null;
    }

    private static void playerHeld(CosmicNivLoadout.Builder builder, CosmicNivService service,
                                   Map<String, NamespacedKey> keys, PlayerInventory inventory,
                                   CosmicNivHeldSearch search) {
        switch (search) {
            case MAIN_HAND -> held(builder, service, keys, inventory.getItemInMainHand());
            case HANDS -> {
                held(builder, service, keys, inventory.getItemInMainHand());
                held(builder, service, keys, inventory.getItemInOffHand());
            }
            case HOTBAR -> {
                for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
                    held(builder, service, keys, inventory.getItem(slot));
                }
                held(builder, service, keys, inventory.getItemInOffHand());
            }
            case INVENTORY -> {
                for (ItemStack stack : inventory.getStorageContents()) {
                    held(builder, service, keys, stack);
                }
                held(builder, service, keys, inventory.getItemInOffHand());
            }
        }
    }

    private static void armor(CosmicNivLoadout.Builder builder, CosmicNivService service,
                              Map<String, NamespacedKey> keys, CosmicNivArmorSlot slot, ItemStack stack) {
        CosmicNivItem item = toItem(stack, service, keys);
        if (item != null) {
            builder.armor(slot, item);
        }
    }

    private static void held(CosmicNivLoadout.Builder builder, CosmicNivService service,
                             Map<String, NamespacedKey> keys, ItemStack stack) {
        CosmicNivItem item = toItem(stack, service, keys);
        if (item != null) {
            builder.held(item);
        }
    }
}
