package dev.linqfy.bigCasares.modules.glider;

import dev.linqfy.bigCasares.items.CatalogBackedCustomItem;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/** Catalog-backed custom item identity for one Glider tier. */
public final class GliderItem extends CatalogBackedCustomItem {

    private final GliderTier tier;

    public GliderItem(GliderTier tier, CustomItemRegistry registry, NamespacedKey legacyItemKey) {
        super(registry, tier.catalogId(), tier.modelData(), legacyItemKey);
        this.tier = Objects.requireNonNull(tier, "tier");
    }

    public GliderTier tier() {
        return tier;
    }

    @Override
    public void onConsume(Player player, ItemStack consumedItem) {
        // A Glider is equipment and is never consumed by ordinary item use.
    }
}
