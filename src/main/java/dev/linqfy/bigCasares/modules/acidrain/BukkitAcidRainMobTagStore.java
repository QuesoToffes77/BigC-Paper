package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

/**
 * Adapts a Bukkit {@link PersistentDataContainer} to the pure
 * {@link AcidRainMobTagStore} used by {@link AcidRainMobIdentity}.
 */
final class BukkitAcidRainMobTagStore implements AcidRainMobTagStore {

    private final PersistentDataContainer container;

    BukkitAcidRainMobTagStore(PersistentDataContainer container) {
        this.container = Objects.requireNonNull(container, "container");
    }

    @Override
    public <T, Z> void set(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        container.set(key, type, value);
    }

    @Override
    public <T, Z> Z get(NamespacedKey key, PersistentDataType<T, Z> type) {
        return container.get(key, type);
    }
}
