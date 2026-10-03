package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Pure identification logic for Acid Rain mobs. Identity is stored in the
 * Bukkit {@code PersistentDataContainer} under the {@code bigcasares}
 * namespace and never depends on the visible display name.
 *
 * <p>The logic operates on {@link AcidRainMobTagStore} so it can be unit
 * tested without a live server; {@link BukkitAcidRainMobTagStore} adapts a
 * real container.
 */
public final class AcidRainMobIdentity {

    public static final String NAMESPACE = "bigcasares";
    public static final String MOB_TAG_KEY = "acidrain_mob";
    public static final String MOB_TYPE_KEY = "acidrain_mob_type";

    private static final NamespacedKey MOB_TAG =
        NamespacedKey.fromString(NAMESPACE + ":" + MOB_TAG_KEY);
    private static final NamespacedKey MOB_TYPE =
        NamespacedKey.fromString(NAMESPACE + ":" + MOB_TYPE_KEY);

    private AcidRainMobIdentity() {
    }

    public static void tag(AcidRainMobTagStore store, AcidRainMobType type) {
        store.set(MOB_TAG, PersistentDataType.BYTE, (byte) 1);
        store.set(MOB_TYPE, PersistentDataType.STRING, type.name());
    }

    public static boolean isAcidRainMob(AcidRainMobTagStore store) {
        return store.get(MOB_TAG, PersistentDataType.BYTE) != null;
    }

    public static Optional<AcidRainMobType> typeOf(AcidRainMobTagStore store) {
        String raw = store.get(MOB_TYPE, PersistentDataType.STRING);
        return AcidRainMobType.parse(raw);
    }

    public static NamespacedKey mobTagKey() {
        return MOB_TAG;
    }

    public static NamespacedKey mobTypeKey() {
        return MOB_TYPE;
    }
}
