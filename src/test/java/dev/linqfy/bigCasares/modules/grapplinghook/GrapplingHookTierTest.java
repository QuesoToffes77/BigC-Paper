package dev.linqfy.bigCasares.modules.grapplinghook;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrapplingHookTierTest {

    @Test
    void hasExactlySixTiers() {
        assertEquals(6, GrapplingHookTier.values().length);
        assertEquals(GrapplingHookTier.I, GrapplingHookTier.values()[0]);
        assertEquals(GrapplingHookTier.VI, GrapplingHookTier.values()[5]);
    }

    @Test
    void catalogIdsAndModelDataAreUnique() {
        Set<String> ids = new HashSet<>();
        Set<Integer> modelData = new HashSet<>();
        for (GrapplingHookTier tier : GrapplingHookTier.values()) {
            assertTrue(ids.add(tier.catalogId()), "duplicate catalog id " + tier.catalogId());
            assertTrue(modelData.add(tier.modelData()), "duplicate model data " + tier.modelData());
        }
    }

    @Test
    void rangeEscalatesPerTier() {
        for (int index = 1; index < GrapplingHookTier.values().length; index++) {
            GrapplingHookTier previous = GrapplingHookTier.values()[index - 1];
            GrapplingHookTier current = GrapplingHookTier.values()[index];
            assertTrue(current.defaultRange() > previous.defaultRange(),
                current + " range must exceed " + previous.defaultRange());
        }
    }

    @Test
    void cooldownDecreasesPerTier() {
        for (int index = 1; index < GrapplingHookTier.values().length; index++) {
            GrapplingHookTier previous = GrapplingHookTier.values()[index - 1];
            GrapplingHookTier current = GrapplingHookTier.values()[index];
            assertTrue(current.defaultCooldownSeconds() < previous.defaultCooldownSeconds(),
                current + " cooldown must be lower than " + previous.defaultCooldownSeconds());
        }
    }

    @Test
    void defaultsMatchApprovedTable() {
        assertEquals(50.0, GrapplingHookTier.I.defaultRange(), 1.0e-9);
        assertEquals(5.0, GrapplingHookTier.I.defaultCooldownSeconds(), 1.0e-9);
        assertEquals(200.0, GrapplingHookTier.VI.defaultRange(), 1.0e-9);
        assertEquals(1.25, GrapplingHookTier.VI.defaultCooldownSeconds(), 1.0e-9);
    }

    @Test
    void resolvesFromCatalogId() {
        assertEquals(GrapplingHookTier.III, GrapplingHookTier.fromCatalogId("grappling_hook_3").orElseThrow());
        assertEquals(GrapplingHookTier.VI, GrapplingHookTier.fromCatalogId("grappling_hook_6").orElseThrow());
        assertTrue(GrapplingHookTier.fromCatalogId("grappling_hook_9").isEmpty());
        assertTrue(GrapplingHookTier.fromCatalogId(null).isEmpty());
        assertTrue(GrapplingHookTier.fromCatalogId("fishing_rod").isEmpty());
    }
}
