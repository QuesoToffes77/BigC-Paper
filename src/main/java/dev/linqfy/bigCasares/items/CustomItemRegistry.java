package dev.linqfy.bigCasares.items;

import dev.linqfy.bigCasares.items.catalog.CatalogItemStackFactory;
import dev.linqfy.bigCasares.items.catalog.CustomItemCatalog;
import dev.linqfy.bigCasares.items.catalog.CustomItemDefinition;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CustomItemRegistry {

    private final Map<Integer, CustomItem> itemsByModelData = new LinkedHashMap<>();
    private final Map<String, CustomItem> itemsById = new LinkedHashMap<>();
    private final CatalogItemStackFactory stackFactory;
    private CustomItemCatalog catalog;

    public CustomItemRegistry() {
        this(new CatalogItemStackFactory());
    }

    public CustomItemRegistry(CatalogItemStackFactory stackFactory) {
        this.stackFactory = java.util.Objects.requireNonNull(stackFactory, "stackFactory");
    }

    public void register(CustomItem item) {
        int modelData = item.getCustomModelData();
        if (itemsByModelData.containsKey(modelData)) {
            throw new IllegalArgumentException("Duplicate CustomModelData: " + modelData);
        }

        String normalizedId = normalizeId(item.getId());
        if (itemsById.containsKey(normalizedId)) {
            throw new IllegalArgumentException("Duplicate custom item id: " + item.getId());
        }

        itemsByModelData.put(modelData, item);
        itemsById.put(normalizedId, item);
    }

    public void unregister(String itemId) {
        String normalizedId = normalizeId(itemId);
        CustomItem item = itemsById.remove(normalizedId);
        if (item != null) {
            itemsByModelData.remove(item.getCustomModelData());
        }
    }

    public Optional<CustomItem> findByModelData(int modelData) {
        return Optional.ofNullable(itemsByModelData.get(modelData));
    }

    public Optional<CustomItem> findById(String itemId) {
        return Optional.ofNullable(itemsById.get(normalizeId(itemId)));
    }

    public Optional<CustomItem> findByItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }

        Optional<String> canonicalId = resolveCanonicalId(itemStack);
        if (canonicalId.isPresent()) {
            return findById(canonicalId.orElseThrow());
        }

        return findByLegacyItemStack(itemStack);
    }

    public Collection<CustomItem> getAllItems() {
        return Collections.unmodifiableCollection(itemsById.values());
    }

    public int getRegisteredItemCount() {
        return itemsById.size();
    }

    public void installCatalog(CustomItemCatalog catalog) {
        this.catalog = java.util.Objects.requireNonNull(catalog, "catalog");
    }

    public Optional<CustomItemCatalog> catalog() {
        return Optional.ofNullable(catalog);
    }

    public ItemStack createItemStack(String itemId, int amount) {
        CustomItemCatalog activeCatalog = catalog;
        if (activeCatalog == null) {
            throw new IllegalStateException("custom item catalog is not active");
        }
        CustomItemDefinition definition = activeCatalog.require(itemId);
        return stackFactory.create(definition, activeCatalog.revision(), amount);
    }

    public Optional<String> resolveItemId(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }
        Optional<String> canonicalId = resolveCanonicalId(itemStack);
        if (canonicalId.isPresent()) {
            return canonicalId;
        }
        return findByLegacyItemStack(itemStack).map(CustomItem::getId);
    }

    public CatalogItemStackFactory stackFactory() {
        return stackFactory;
    }

    private Optional<String> resolveCanonicalId(ItemStack itemStack) {
        CustomItemCatalog activeCatalog = catalog;
        if (activeCatalog == null || itemStack == null || !itemStack.hasItemMeta()) {
            return Optional.empty();
        }
        String rawId = itemStack.getItemMeta().getPersistentDataContainer().get(
            stackFactory.itemIdKey(), PersistentDataType.STRING);
        if (rawId == null) {
            return Optional.empty();
        }
        return activeCatalog.find(rawId).map(CustomItemDefinition::id);
    }

    private Optional<CustomItem> findByLegacyItemStack(ItemStack itemStack) {
        var meta = itemStack.getItemMeta();
        var modelData = ModelDataUtil.readCustomModelData(meta);
        if (modelData.isEmpty()) {
            return Optional.empty();
        }

        CustomItem candidate = itemsByModelData.get(modelData.getAsInt());
        if (candidate == null || !candidate.matchesLegacy(itemStack)) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    private String normalizeId(String itemId) {
        return itemId.toLowerCase(Locale.ROOT).trim();
    }
}
