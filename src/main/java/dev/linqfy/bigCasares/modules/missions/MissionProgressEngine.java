package dev.linqfy.bigCasares.modules.missions;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public final class MissionProgressEngine {

    private MissionProgressEngine() {
    }

    public static int resolveAbsoluteProgress(MissionDefinition definition, Map<String, Object> context) {
        return switch (definition.type()) {
            case HOLD_EXACT_ITEM_COUNT, WAX_BLOCK_COUNT, CROUCH_ON_SLEEPING_BED -> asInt(context.getOrDefault("count", 0));
            case EQUIP_SPECIFIC_ITEM, STAND_ON_BLOCK_AT_Y, RENAME_ITEM_TO_EXACT_NAME, NAME_ENTITY_AFTER_PLAYER,
                FINAL_HIT_PLAYER_WITH_ITEM, KILL_ENTITY_WITH_ITEM_ONLY, VISIT_BIOME, VISIT_BIOME_SET,
                REACH_DISTANCE_FROM_SPAWN, ENTER_ENVIRONMENT, OPEN_LOOT_TABLE, CATCH_FISH_IN_BIOME,
                KILL_ENTITY_IN_BIOME -> asInt(context.getOrDefault("count", 0));
        };
    }

    public static boolean isCompleted(MissionDefinition definition, int progress) {
        return switch (definition.type()) {
            case HOLD_EXACT_ITEM_COUNT -> progress == definition.goal();
            default -> progress >= definition.goal();
        };
    }

    public static int countInventoryMaterial(Player player, Material material) {
        int total = 0;
        PlayerInventory inventory = player.getInventory();
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    public static boolean isWearing(Player player, Material material) {
        ItemStack helmet = player.getInventory().getHelmet();
        return helmet != null && helmet.getType() == material;
    }

    public static boolean isWearingCursed(Player player, Material material) {
        ItemStack helmet = player.getInventory().getHelmet();
        return helmet != null
            && helmet.getType() == material
            && helmet.getEnchantments().containsKey(org.bukkit.enchantments.Enchantment.BINDING_CURSE);
    }

    public static boolean isStandingOn(Player player, Material material, int yLevel) {
        Block block = player.getLocation().clone().subtract(0, 1, 0).getBlock();
        return player.getLocation().getBlockY() == yLevel && block.getType() == material;
    }

    private static int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
