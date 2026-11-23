package dev.linqfy.bigCasares.modules.villagermood;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadedVillagerIndexTest {
    @Test
    void scansSnapshotWithoutCopyingAndIgnoresLaterAdds() {
        LoadedVillagerIndex<String> index = new LoadedVillagerIndex<>();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        index.add(a, "a"); index.add(b, "b");
        long limit = index.watermark();
        assertEquals("a", index.next(0, limit).orElseThrow().value());
        index.add(c, "c");
        var second = index.next(1, limit).orElseThrow();
        assertEquals("b", second.value());
        assertTrue(index.next(second.sequence(), limit).isEmpty());
    }

    @Test
    void unloadAndDuplicateLoadDoNotLeaveStaleEntries() {
        LoadedVillagerIndex<String> index = new LoadedVillagerIndex<>();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        index.add(a, "a"); index.add(a, "duplicate"); index.add(b, "b");
        index.remove(a);
        assertEquals(1, index.size());
        assertEquals("b", index.next(0, index.watermark()).orElseThrow().value());
        index.clear();
        assertTrue(index.next(0, Long.MAX_VALUE).isEmpty());
    }
    @Test
    void staleRemovalCannotDiscardReplacementWithSameId() {
        LoadedVillagerIndex<Object> index = new LoadedVillagerIndex<>();
        UUID id = UUID.randomUUID();
        Object oldEntity = new Object(), newEntity = new Object();
        index.add(id, oldEntity);
        index.add(id, newEntity);
        assertFalse(index.remove(id, oldEntity));
        assertSame(newEntity, index.next(0, index.watermark()).orElseThrow().value());
        assertTrue(index.remove(id, newEntity));
    }
}
