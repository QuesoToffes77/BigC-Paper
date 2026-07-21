package dev.linqfy.bigCasares.modules.warp;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class WarpMenuHolder implements InventoryHolder {
    private Inventory inventory;

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
