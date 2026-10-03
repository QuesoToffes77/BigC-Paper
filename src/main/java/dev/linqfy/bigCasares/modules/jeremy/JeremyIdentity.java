package dev.linqfy.bigCasares.modules.jeremy;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

final class JeremyIdentity {
    static final String CUSTOM_ENTITY_KEY = "custom_entity";
    static final String ENTITY_ID = "jeremy";

    private final NamespacedKey identityKey;
    private final NamespacedKey targetKey;

    JeremyIdentity(Plugin plugin) {
        identityKey = new NamespacedKey(plugin, CUSTOM_ENTITY_KEY);
        targetKey = new NamespacedKey(plugin, "jeremy_target");
    }

    void mark(Entity entity, UUID targetUuid) {
        entity.getPersistentDataContainer().set(identityKey, PersistentDataType.STRING, ENTITY_ID);
        if (targetUuid != null) {
            entity.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, targetUuid.toString());
        }
    }

    boolean isJeremy(Entity entity) {
        if (entity == null) {
            return false;
        }
        return ENTITY_ID.equals(entity.getPersistentDataContainer().get(identityKey, PersistentDataType.STRING));
    }
}
