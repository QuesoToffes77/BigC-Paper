package dev.linqfy.bigCasares.items.catalog;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

public final class OnlineItemReconciler implements Runnable {

    private final Plugin plugin;
    private final CustomItemRegistry registry;
    private final CatalogItemStackFactory stackFactory;
    private final ItemReconciliationPolicy policy;
    private final int stacksPerTick;
    private final Queue<InventorySlot> pending = new ArrayDeque<>();
    private final NamespacedKey legacyItemKey = new NamespacedKey("bigcasares", "legacy_item_id");

    private CustomItemCatalog previous;
    private CustomItemCatalog candidate;
    private int refreshed;
    private int legacyMarked;
    private int ignored;

    public OnlineItemReconciler(
        Plugin plugin,
        CustomItemRegistry registry,
        int stacksPerTick
    ) {
        this(plugin, registry, new ItemReconciliationPolicy(), stacksPerTick);
    }

    OnlineItemReconciler(
        Plugin plugin,
        CustomItemRegistry registry,
        ItemReconciliationPolicy policy,
        int stacksPerTick
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.stackFactory = registry.stackFactory();
        this.policy = Objects.requireNonNull(policy, "policy");
        if (stacksPerTick < 1) {
            throw new IllegalArgumentException("stacksPerTick must be positive");
        }
        this.stacksPerTick = stacksPerTick;
    }

    public void request(CustomItemCatalog previous, CustomItemCatalog candidate) {
        this.previous = previous;
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        pending.clear();
        refreshed = 0;
        legacyMarked = 0;
        ignored = 0;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            queue(player.getInventory());
            queue(player.getEnderChest());
        }
    }

    @Override
    public void run() {
        CustomItemCatalog activeCandidate = candidate;
        if (activeCandidate == null) {
            return;
        }
        for (int count = 0; count < stacksPerTick && !pending.isEmpty(); count++) {
            reconcile(pending.remove(), activeCandidate);
        }
    }

    public ItemReconciliationResult result() {
        return new ItemReconciliationResult(refreshed, legacyMarked, ignored, pending.size());
    }

    public void clear() {
        pending.clear();
        previous = null;
        candidate = null;
    }

    private void queue(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack != null && stack.hasItemMeta()) {
                pending.add(new InventorySlot(inventory, slot));
            }
        }
    }

    private void reconcile(InventorySlot slot, CustomItemCatalog activeCandidate) {
        ItemStack stack = slot.inventory().getItem(slot.slot());
        if (stack == null || !stack.hasItemMeta()) {
            return;
        }
        Optional<String> logicalId = resolveLogicalId(stack);
        if (logicalId.isEmpty()) {
            ignored++;
            return;
        }
        ItemReconciliationDecision decision = policy.decide(logicalId.orElseThrow(), previous, activeCandidate);
        switch (decision) {
            case REFRESH -> refresh(stack, activeCandidate.require(logicalId.orElseThrow()));
            case MARK_LEGACY -> markLegacy(stack, logicalId.orElseThrow());
            case IGNORE -> ignored++;
        }
    }

    private Optional<String> resolveLogicalId(ItemStack stack) {
        String canonical = stack.getItemMeta().getPersistentDataContainer().get(
            stackFactory.itemIdKey(), PersistentDataType.STRING
        );
        if (canonical != null && !canonical.isBlank()) {
            return Optional.of(canonical.trim().toLowerCase(Locale.ROOT));
        }
        return registry.resolveItemId(stack);
    }

    private void refresh(ItemStack stack, CustomItemDefinition definition) {
        if (stackFactory.reconcile(stack, definition, candidate.revision())) {
            refreshed++;
        } else {
            markLegacy(stack, definition.id());
        }
    }

    private void markLegacy(ItemStack stack, String logicalId) {
        var meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(legacyItemKey, PersistentDataType.STRING, logicalId);
        stack.setItemMeta(meta);
        legacyMarked++;
    }

    private record InventorySlot(Inventory inventory, int slot) {
    }
}
