package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

public final class CustomCrossbowData {

    private final NamespacedKey chargeTypeKey;
    private final NamespacedKey fireworkPowerKey;
    private final NamespacedKey chargeCountKey;
    private final NamespacedKey originalItemModelKey;

    public CustomCrossbowData(
        NamespacedKey chargeTypeKey,
        NamespacedKey fireworkPowerKey,
        NamespacedKey chargeCountKey,
        NamespacedKey originalItemModelKey
    ) {
        this.chargeTypeKey = chargeTypeKey;
        this.fireworkPowerKey = fireworkPowerKey;
        this.chargeCountKey = chargeCountKey;
        this.originalItemModelKey = originalItemModelKey;
    }

    public void writeCharge(ItemStack crossbow, CustomCrossbowChargeType chargeType, int fireworkPower, int chargeCount) {
        if (crossbow == null || crossbow.getType() != Material.CROSSBOW) {
            return;
        }

        ItemMeta rawMeta = crossbow.getItemMeta();
        if (!(rawMeta instanceof CrossbowMeta meta)) {
            return;
        }

        meta.getPersistentDataContainer().set(chargeTypeKey, PersistentDataType.STRING, chargeType.name());
        meta.getPersistentDataContainer().set(fireworkPowerKey, PersistentDataType.INTEGER, Math.max(0, fireworkPower));
        meta.getPersistentDataContainer().set(chargeCountKey, PersistentDataType.INTEGER, Math.max(1, chargeCount));
        if (!meta.getPersistentDataContainer().has(originalItemModelKey, PersistentDataType.STRING)) {
            String originalModel = meta.hasItemModel() ? meta.getItemModel().toString() : "";
            meta.getPersistentDataContainer().set(originalItemModelKey, PersistentDataType.STRING, originalModel);
        }
        meta.setItemModel(new NamespacedKey("bigcasares", CustomCrossbowRules.loadedCrossbowModelId(chargeType)));
        meta.setChargedProjectiles(List.of(placeholderProjectile(chargeType, fireworkPower)));
        crossbow.setItemMeta(meta);
    }

    public Optional<StoredCrossbowCharge> readCharge(ItemStack crossbow) {
        if (crossbow == null || crossbow.getType() != Material.CROSSBOW || !crossbow.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = crossbow.getItemMeta();
        String rawType = meta.getPersistentDataContainer().get(chargeTypeKey, PersistentDataType.STRING);
        if (rawType == null) {
            return Optional.empty();
        }

        try {
            CustomCrossbowChargeType type = CustomCrossbowChargeType.valueOf(rawType);
            Integer fireworkPower = meta.getPersistentDataContainer().get(fireworkPowerKey, PersistentDataType.INTEGER);
            Integer chargeCount = meta.getPersistentDataContainer().get(chargeCountKey, PersistentDataType.INTEGER);
            return Optional.of(new StoredCrossbowCharge(type, fireworkPower == null ? 0 : fireworkPower, chargeCount == null ? 1 : chargeCount));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public boolean isEchoChargedCrossbow(ItemStack itemStack) {
        return readCharge(itemStack)
            .map(charge -> charge.type() == CustomCrossbowChargeType.ECHO_SHARD)
            .orElse(false);
    }

    public void clearCharge(ItemStack crossbow) {
        if (crossbow == null || crossbow.getType() != Material.CROSSBOW || !crossbow.hasItemMeta()) {
            return;
        }

        ItemMeta rawMeta = crossbow.getItemMeta();
        rawMeta.getPersistentDataContainer().remove(chargeTypeKey);
        rawMeta.getPersistentDataContainer().remove(fireworkPowerKey);
        rawMeta.getPersistentDataContainer().remove(chargeCountKey);
        restoreItemModel(rawMeta);
        if (rawMeta instanceof CrossbowMeta meta) {
            meta.setChargedProjectiles(List.of());
            crossbow.setItemMeta(meta);
            return;
        }
        crossbow.setItemMeta(rawMeta);
    }

    private ItemStack placeholderProjectile(CustomCrossbowChargeType chargeType, int fireworkPower) {
        if (chargeType != CustomCrossbowChargeType.FIREWORK_ROCKET) {
            return new ItemStack(Material.ARROW);
        }

        ItemStack firework = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta rawMeta = firework.getItemMeta();
        if (rawMeta instanceof FireworkMeta meta) {
            meta.setPower(Math.max(0, Math.min(3, fireworkPower)));
            firework.setItemMeta(meta);
        }
        return firework;
    }

    private void restoreItemModel(ItemMeta meta) {
        String originalModel = meta.getPersistentDataContainer().get(originalItemModelKey, PersistentDataType.STRING);
        meta.getPersistentDataContainer().remove(originalItemModelKey);
        if (originalModel == null) {
            return;
        }
        if (originalModel.isBlank()) {
            meta.setItemModel(null);
            return;
        }
        meta.setItemModel(NamespacedKey.fromString(originalModel));
    }
}
