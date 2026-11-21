package dev.linqfy.bigCasares.modules.auth;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryRestoreCheckpointTest {
    @Test
    void appliesInventoryBeforeCheckpointAndPropagatesFailure() {
        List<String> calls = new ArrayList<>();
        assertThrows(IllegalStateException.class, () -> InventoryRestoreCheckpoint.restore(false,
            () -> calls.add("apply"), () -> {
                calls.add("checkpoint");
                throw new IOException("not saved");
            }));
        assertEquals(List.of("apply", "checkpoint"), calls);
    }

    @Test
    void matchingInMemoryMarkerStillRequiresFreshCheckpoint() {
        List<String> calls = new ArrayList<>();
        InventoryRestoreCheckpoint.restore(true, () -> calls.add("apply"), () -> calls.add("checkpoint"));
        assertEquals(List.of("checkpoint"), calls);
    }
}
