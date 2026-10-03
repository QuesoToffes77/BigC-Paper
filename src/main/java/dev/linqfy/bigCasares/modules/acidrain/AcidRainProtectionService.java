package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class AcidRainProtectionService {
    private AcidRainProtectionSettings settings;

    AcidRainProtectionService(AcidRainProtectionSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    void updateSettings(AcidRainProtectionSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    double protectionFor(Player player) {
        Set<Material> armor = Arrays.stream(player.getInventory().getArmorContents())
            .filter(stack -> stack != null && stack.getType() != Material.AIR)
            .map(ItemStack::getType)
            .collect(Collectors.toUnmodifiableSet());
        Set<Material> inventory = Arrays.stream(player.getInventory().getContents())
            .filter(stack -> stack != null && stack.getType() != Material.AIR)
            .map(ItemStack::getType)
            .collect(Collectors.toUnmodifiableSet());

        double best = 0.0;
        for (AcidRainProtectionGroup group : settings.groups()) {
            boolean hasArmor = group.requiredArmor().isEmpty() || armor.containsAll(group.requiredArmor());
            boolean hasCarried = group.carriedMaterials().isEmpty() || inventory.stream().anyMatch(group.carriedMaterials()::contains);
            if ((group.requiredArmor().isEmpty() && group.carriedMaterials().isEmpty()) || !hasArmor || !hasCarried) {
                continue;
            }
            best = Math.max(best, group.protectionPercent());
        }
        return Math.max(0.0, Math.min(100.0, best));
    }
}
