package dev.linqfy.bigCasares.modules.moderation.anticheat;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanBatchTest {
    @Test void combinesFairWindowWithIndependentPartnersWithoutDuplicates() {
        var players = IntStream.range(0, 50).boxed().toList();
        var first = ScanBatch.select(players, 0, 16, new Random(1));
        var second = ScanBatch.select(players, 0, 16, new Random(2));
        assertEquals(16, first.size());
        assertEquals(16, first.stream().distinct().count());
        assertEquals(players.subList(0, 8), first.subList(0, 8));
        org.junit.jupiter.api.Assertions.assertNotEquals(first.subList(8, 16), second.subList(8, 16));
        var rotated = ScanBatch.select(players, 8, 16, new Random(1));
        assertEquals(players.subList(8, 16), rotated.subList(0, 8));
    }

    @Test void limitsOnePassAndRotatesWithoutDuplicatePlayers() {
        var players = List.of("a", "b", "c", "d", "e");
        assertEquals(List.of("a", "b", "c"), ScanBatch.select(players, 0, 3));
        assertEquals(List.of("e", "a", "b"), ScanBatch.select(players, 4, 3));
        assertEquals(players, ScanBatch.select(players, 0, 9));
        assertEquals(List.of(), ScanBatch.select(List.of(), 0, 3));
    }
}
