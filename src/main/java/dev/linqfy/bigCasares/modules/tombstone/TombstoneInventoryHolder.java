package dev.linqfy.bigCasares.modules.tombstone;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public record TombstoneInventoryHolder(UUID tombstoneId) implements InventoryHolder {
    @Override public Inventory getInventory() { return null; }
}
