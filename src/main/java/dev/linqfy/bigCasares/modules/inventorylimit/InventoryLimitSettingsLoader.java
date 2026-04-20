package dev.linqfy.bigCasares.modules.inventorylimit;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public final class InventoryLimitSettingsLoader {

    public Map<Material, Integer> load(ConfigurationSection config) {
        ConfigurationSection limitsSection = config.getConfigurationSection("inventory-limit.limits");
        if (limitsSection == null) {
            return Map.of();
        }

        Map<Material, Integer> limits = new LinkedHashMap<>();
        for (String key : limitsSection.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                throw new IllegalArgumentException("Material invalido: " + key);
            }

            int limit = limitsSection.getInt(key);
            if (limit <= 0) {
                throw new IllegalArgumentException("El limite debe ser mayor que cero para " + key);
            }

            limits.put(material, limit);
        }
        return Map.copyOf(limits);
    }
}
