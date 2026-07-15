package dev.linqfy.bigCasares.modules.servercontrol;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ControlMenuHolder implements InventoryHolder {
    public enum Menu {
        MAIN,
        PVP
    }

    private final Menu menu;
    private Inventory inventory;

    public ControlMenuHolder(Menu menu) {
        this.menu = menu;
    }

    public Menu menu() {
        return menu;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
