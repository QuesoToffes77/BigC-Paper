package dev.linqfy.bigCasares.items;

import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CustomItemRegistry {

    private final Map<Integer, CustomItem> itemsByModelData = new LinkedHashMap<>();
    private final Map<String, CustomItem> itemsById = new LinkedHashMap<>();

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

        var meta = itemStack.getItemMeta();
        var modelData = ModelDataUtil.readCustomModelData(meta);
        if (modelData.isEmpty()) {
            return Optional.empty();
        }

        CustomItem candidate = itemsByModelData.get(modelData.getAsInt());
        if (candidate == null || !candidate.matches(itemStack)) {
            return Optional.empty();
        }

        return Optional.of(candidate);
    }

    public Collection<CustomItem> getAllItems() {
        return Collections.unmodifiableCollection(itemsById.values());
    }

    public int getRegisteredItemCount() {
        return itemsById.size();
    }

    private String normalizeId(String itemId) {
        return itemId.toLowerCase(Locale.ROOT).trim();
    }
}
