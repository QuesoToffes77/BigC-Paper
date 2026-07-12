package dev.linqfy.bigCasares.modules.shop;

import org.bukkit.entity.Pose;

import java.util.Map;
import java.util.Objects;

public record ShopNpcDefinition(
    String shopId,
    ShopNpcType type,
    String displayName,
    ShopSkinSource skinSource,
    String skinValue,
    Pose pose,
    boolean lookAtPlayer,
    boolean immovable,
    Map<String, String> equipment,
    String profession,
    String biomeType,
    int villagerLevel,
    boolean baby
) {
    public ShopNpcDefinition {
        shopId = requireText(shopId, "shopId");
        type = Objects.requireNonNull(type, "type");
        displayName = requireText(displayName, "displayName");
        pose = pose == null ? Pose.STANDING : pose;
        equipment = Map.copyOf(equipment == null ? Map.of() : equipment);
        profession = profession == null || profession.isBlank() ? "none" : profession.trim().toLowerCase(java.util.Locale.ROOT);
        biomeType = biomeType == null || biomeType.isBlank() ? "plains" : biomeType.trim().toLowerCase(java.util.Locale.ROOT);
        if (villagerLevel < 1 || villagerLevel > 5) {
            throw new IllegalArgumentException("villagerLevel must be between 1 and 5");
        }
        if (type == ShopNpcType.PLAYER_MODEL) {
            skinSource = Objects.requireNonNull(skinSource, "skinSource");
            skinValue = requireText(skinValue, "skinValue");
        }
    }

    public ShopNpcType bedrockFallback() {
        return ShopNpcType.VILLAGER;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
