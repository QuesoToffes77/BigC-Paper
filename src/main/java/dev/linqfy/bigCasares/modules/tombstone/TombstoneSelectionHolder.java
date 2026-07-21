package dev.linqfy.bigCasares.modules.tombstone;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TombstoneSelectionHolder implements InventoryHolder {
    private final List<UUID> tombstoneIds;

    public TombstoneSelectionHolder(List<UUID> tombstoneIds) {
        this.tombstoneIds = List.copyOf(tombstoneIds);
    }

    public Optional<UUID> tombstoneAt(int slot) {
        return slot >= 0 && slot < tombstoneIds.size()
            ? Optional.of(tombstoneIds.get(slot)) : Optional.empty();
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
