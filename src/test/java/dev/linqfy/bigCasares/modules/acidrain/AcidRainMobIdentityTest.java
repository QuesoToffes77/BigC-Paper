package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AcidRainMobIdentityTest {

    @Test
    void normalMobIsNotAnAcidRainMob() {
        FakeStore store = new FakeStore();

        assertFalse(AcidRainMobIdentity.isAcidRainMob(store));
        assertTrue(AcidRainMobIdentity.typeOf(store).isEmpty());
    }

    @Test
    void everyMobTypeIsIdentifiedCorrectly() {
        for (AcidRainMobType type : AcidRainMobType.values()) {
            FakeStore store = new FakeStore();

            AcidRainMobIdentity.tag(store, type);

            assertTrue(AcidRainMobIdentity.isAcidRainMob(store), "expected tag for " + type);
            assertEquals(Optional.of(type), AcidRainMobIdentity.typeOf(store));
        }
    }

    @Test
    void unknownTypeTagIsRejected() {
        FakeStore store = new FakeStore();
        store.set(AcidRainMobIdentity.mobTypeKey(), PersistentDataType.STRING, "GHOST");

        assertFalse(AcidRainMobIdentity.isAcidRainMob(store));
        assertTrue(AcidRainMobIdentity.typeOf(store).isEmpty());
    }

    @Test
    void identityUsesTheBigCasaresNamespace() {
        assertEquals("bigcasares", AcidRainMobIdentity.NAMESPACE);
        assertEquals("bigcasares:acidrain_mob", AcidRainMobIdentity.mobTagKey().toString());
        assertEquals("bigcasares:acidrain_mob_type", AcidRainMobIdentity.mobTypeKey().toString());
    }

    private static final class FakeStore implements AcidRainMobTagStore {

        private final Map<String, Object> values = new HashMap<>();

        @Override
        public <T, Z> void set(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
            values.put(key.toString(), value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T, Z> Z get(NamespacedKey key, PersistentDataType<T, Z> type) {
            return (Z) values.get(key.toString());
        }
    }
}
