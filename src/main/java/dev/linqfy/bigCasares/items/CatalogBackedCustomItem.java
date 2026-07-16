package dev.linqfy.bigCasares.items;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public abstract class CatalogBackedCustomItem implements CustomItem {

    private final CustomItemRegistry registry;
    private final String id;
    private final int legacyModelData;
    private final NamespacedKey legacyItemKey;

    protected CatalogBackedCustomItem(
        CustomItemRegistry registry,
        String id,
        int legacyModelData,
        NamespacedKey legacyItemKey
    ) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.id = Objects.requireNonNull(id, "id");
        this.legacyModelData = legacyModelData;
        this.legacyItemKey = Objects.requireNonNull(legacyItemKey, "legacyItemKey");
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final int getCustomModelData() {
        return legacyModelData;
    }

    @Override
    public final NamespacedKey getItemKey() {
        return legacyItemKey;
    }

    @Override
    public final ItemStack createItemStack(int amount) {
        return registry.createItemStack(id, amount);
    }

    @Override
    public final boolean matches(ItemStack item) {
        return registry.resolveItemId(item).map(id::equals).orElse(false);
    }
}
