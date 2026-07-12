package dev.linqfy.bigCasares.module;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class ModuleManager {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final Map<String, PluginModule> registeredModules = new LinkedHashMap<>();
    private final Map<String, PluginModule> activeModules = new LinkedHashMap<>();

    public ModuleManager(JavaPlugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void register(PluginModule module) {
        String normalizedId = normalizeModuleId(module.getId());
        if (registeredModules.containsKey(normalizedId)) {
            throw new IllegalArgumentException("Duplicate module id: " + module.getId());
        }
        registeredModules.put(normalizedId, module);
    }

    public void enableRegisteredModules() {
        for (Map.Entry<String, PluginModule> entry : registeredModules.entrySet()) {
            String moduleId = entry.getKey();
            PluginModule module = entry.getValue();

            if (!isEnabledInConfig(moduleId)) {
                plugin.getLogger().info("Module disabled by config: " + moduleId);
                continue;
            }

            try {
                module.onEnable();
                activeModules.put(moduleId, module);
                plugin.getLogger().info("Enabled module: " + moduleId);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to enable module: " + moduleId, ex);
            }
        }
    }

    public void disableActiveModules() {
        List<Map.Entry<String, PluginModule>> entries = new ArrayList<>(activeModules.entrySet());
        for (int i = entries.size() - 1; i >= 0; i--) {
            String moduleId = entries.get(i).getKey();
            PluginModule module = entries.get(i).getValue();
            try {
                module.onDisable();
                plugin.getLogger().info("Disabled module: " + moduleId);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to disable module: " + moduleId, ex);
            }
        }
        activeModules.clear();
    }

    public int getRegisteredModuleCount() {
        return registeredModules.size();
    }

    public int getActiveModuleCount() {
        return activeModules.size();
    }

    private boolean isEnabledInConfig(String moduleId) {
        return isEnabledInConfig(config, moduleId);
    }

    static boolean isEnabledInConfig(ConfigurationSection config, String moduleId) {
        String nestedPath = "modules." + moduleId + ".enabled";
        if (config.isSet(nestedPath)) {
            return config.getBoolean(nestedPath);
        }
        String flatPath = "modules." + moduleId;
        if (config.isBoolean(flatPath)) {
            return config.getBoolean(flatPath);
        }
        if ("entity-shop-system".equals(moduleId)) {
            String legacyPath = "modules.shop-system";
            if (config.isBoolean(legacyPath)) {
                return config.getBoolean(legacyPath);
            }
        }
        return true;
    }

    private String normalizeModuleId(String moduleId) {
        return moduleId.toLowerCase(Locale.ROOT).trim();
    }
}
