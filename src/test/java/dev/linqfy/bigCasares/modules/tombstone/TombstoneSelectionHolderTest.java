package dev.linqfy.bigCasares.modules.tombstone;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TombstoneSelectionHolderTest {

    @Test
    void mapsSelectionSlotsToTombstoneIds() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        TombstoneSelectionHolder holder = new TombstoneSelectionHolder(List.of(first, second));

        assertEquals(first, holder.tombstoneAt(0).orElseThrow());
        assertEquals(second, holder.tombstoneAt(1).orElseThrow());
        assertTrue(holder.tombstoneAt(2).isEmpty());
    }
}
