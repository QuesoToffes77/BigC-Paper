package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

/**
 * Minimal read/write view of a persistent tag container, so Acid Rain mob
 * identification can be tested purely without a live server.
 */
public interface AcidRainMobTagStore {

    <T, Z> void set(NamespacedKey key, PersistentDataType<T, Z> type, Z value);

    <T, Z> Z get(NamespacedKey key, PersistentDataType<T, Z> type);
}
