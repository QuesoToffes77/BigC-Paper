package dev.linqfy.bigCasares.modules.missions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MissionCatalogLoader {

    public MissionCatalog load(FileConfiguration config) {
        ConfigurationSection root = config.getConfigurationSection("mission-system.missions");
        if (root == null) {
            return MissionCatalog.of(List.of());
        }

        List<MissionDefinition> definitions = new ArrayList<>();
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            ConfigurationSection paramsSection = section.getConfigurationSection("params");
            Map<String, Object> params = paramsSection == null ? Map.of() : paramsSection.getValues(false);

            definitions.add(new MissionDefinition(
                id,
                MissionScope.valueOf(section.getString("scope", "daily").toUpperCase()),
                section.getString("title", id),
                section.getString("description", ""),
                MissionType.valueOf(section.getString("type", "HOLD_EXACT_ITEM_COUNT")),
                section.getInt("goal"),
                section.getDouble("reward"),
                section.getInt("weight", 1),
                section.getBoolean("enabled", true),
                params
            ));
        }

        return MissionCatalog.of(definitions);
    }
}
