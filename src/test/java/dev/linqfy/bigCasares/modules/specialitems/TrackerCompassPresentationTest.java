package dev.linqfy.bigCasares.modules.specialitems;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackerCompassPresentationTest {

    @Test
    void activePresentationShowsTargetRoundedTimeAndGlint() {
        TrackerCompassPresentation presentation = TrackerCompassPresentation.tracking("Mila", 44_001L);

        assertEquals("§bBrújula Rastreadora §7- §fMila", presentation.displayName());
        assertEquals(List.of("§7Objetivo: §fMila", "§7Rastreo restante: §b45s"), presentation.lore());
        assertTrue(presentation.glint());
    }

    @Test
    void cooldownPresentationShowsRoundedTimeWithoutGlint() {
        TrackerCompassPresentation presentation = TrackerCompassPresentation.cooldown(59_001L);

        assertEquals("§7Brújula Rastreadora", presentation.displayName());
        assertEquals(List.of("§7Recarga restante: §c60s"), presentation.lore());
        assertFalse(presentation.glint());
    }
}
