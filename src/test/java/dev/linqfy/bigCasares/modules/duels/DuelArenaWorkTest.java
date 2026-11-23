package dev.linqfy.bigCasares.modules.duels;
import org.junit.jupiter.api.Test;
import org.bukkit.Material;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
class DuelArenaWorkTest {
    @Test void clearIsBoundedAndCoversTheWholeFootprint() {
        DuelArenaWork work = new DuelArenaWork();
        var visited = new ArrayList<String>();
        while (!work.done()) {
            int before = visited.size();
            work.step(500, (x, y, z) -> visited.add(x + "," + y + "," + z));
            assertTrue(visited.size() - before <= 500);
        }
        assertEquals(69 * 69 * 17, visited.size());
        assertEquals(visited.size(), visited.stream().distinct().count());
        assertTrue(visited.contains("-34,62,-34"));
        assertTrue(visited.contains("34,78,34"));
    }

    @Test void buildsLargestArenaInBoundedBatchesWithoutChangingGeometry() {
        var work = new DuelArenaWork.Build(DuelArenaSize.GRANDE);
        var placements = new ArrayList<String>();
        while (!work.done()) {
            int before = placements.size();
            work.step(128, (x, y, z, material) -> placements.add(x + "," + y + "," + z + "," + material));
            assertTrue(placements.size() - before <= 128);
        }
        assertEquals(64 * 64 + 13 * 66 * 4, placements.size());
        assertTrue(placements.contains("-32,64,-32," + Material.POLISHED_BLACKSTONE_BRICKS));
        assertTrue(placements.contains("0,64,0," + Material.RED_CONCRETE));
        assertTrue(placements.contains("32,76,0," + Material.BARRIER));
    }

    @Test void everySizeHasTheSameFloorAndWallBounds() {
        for (DuelArenaSize size : DuelArenaSize.values()) {
            var work = new DuelArenaWork.Build(size);
            int side = (int) Math.ceil(size.halfSide()) * 2;
            var positions = new ArrayList<String>();
            while (!work.done()) work.step(200, (x, y, z, material) -> positions.add(x + "," + y + "," + z + "," + material));
            assertEquals(side * side + 13 * (side + 2) * 4, positions.size());
            assertTrue(positions.contains((side / 2) + ",76,0," + Material.BARRIER));
        }
    }

    @Test void failedBuildActionDoesNotAdvanceTheCursor() {
        var work = new DuelArenaWork.Build(DuelArenaSize.CHICA);
        assertThrows(IllegalStateException.class, () -> work.step(1, (x, y, z, material) -> { throw new IllegalStateException(); }));
        var positions = new ArrayList<String>();
        work.step(1, (x, y, z, material) -> positions.add(x + "," + y + "," + z));
        assertEquals(java.util.List.of("-12,64,-12"), positions);
    }

    @Test void failedClearActionDoesNotAdvanceTheCursor() {
        var work = new DuelArenaWork();
        assertThrows(IllegalStateException.class, () -> work.step(1, (x, y, z) -> { throw new IllegalStateException(); }));
        var positions = new ArrayList<String>();
        work.step(1, (x, y, z) -> positions.add(x + "," + y + "," + z));
        assertEquals(java.util.List.of("-34,62,-34"), positions);
    }
}
