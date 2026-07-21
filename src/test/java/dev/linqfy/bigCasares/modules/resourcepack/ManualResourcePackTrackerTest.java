package dev.linqfy.bigCasares.modules.resourcepack;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManualResourcePackTrackerTest {

    @Test
    void marksManualPlayersAtTheRevisionTheyWereGiven() throws Exception {
        Class<?> tracker = Class.forName(
            "dev.linqfy.bigCasares.modules.resourcepack.ManualResourcePackTracker"
        );
        Method tagsFor = tracker.getDeclaredMethod("tagsFor", Set.class, String.class);

        @SuppressWarnings("unchecked")
        Set<String> tags = (Set<String>) tagsFor.invoke(null,
            Set.of("some-other-tag", "bigcasares_resourcepack_revision_old"),
            "0123456789abcdef");

        assertTrue(tags.contains("some-other-tag"));
        assertTrue(tags.contains("bigcasares_resourcepack_manual"));
        assertTrue(tags.contains("bigcasares_resourcepack_revision_0123456789abcdef"));
        assertFalse(tags.contains("bigcasares_resourcepack_revision_old"));
    }

    @Test
    void onlyManualPlayersWithAnOldRevisionNeedAnUpdateNotice() throws Exception {
        Class<?> tracker = Class.forName(
            "dev.linqfy.bigCasares.modules.resourcepack.ManualResourcePackTracker"
        );
        Method needsUpdate = tracker.getDeclaredMethod("needsUpdate", Set.class, String.class);

        assertEquals(false, needsUpdate.invoke(null, Set.of(), "0123456789abcdef"));
        assertEquals(false, needsUpdate.invoke(null, Set.of(
            "bigcasares_resourcepack_manual",
            "bigcasares_resourcepack_revision_0123456789abcdef"
        ), "0123456789abcdef"));
        assertEquals(true, needsUpdate.invoke(null, Set.of(
            "bigcasares_resourcepack_manual",
            "bigcasares_resourcepack_revision_fedcba9876543210"
        ), "0123456789abcdef"));
    }
}
