package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.items.CatalogBackedCustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Catalog-backed handle for one Grappling Hook tier. Identity is the catalog
 * persistent-data id; this class only links a tier to its registry entry.
 */
public final class GrapplingHookItem extends CatalogBackedCustomItem {

    private final GrapplingHookTier tier;

    public GrapplingHookItem(GrapplingHookTier tier, CustomItemRegistry registry, NamespacedKey legacyItemKey) {
        super(registry, tier.catalogId(), tier.modelData(), legacyItemKey);
        this.tier = Objects.requireNonNull(tier, "tier");
    }

    public GrapplingHookTier tier() {
        return tier;
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
        // Hooks are used by right-click, not consumed as food.
    }
}
