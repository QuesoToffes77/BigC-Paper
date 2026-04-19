package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionProgressEngineTest {

    @Test
    void returnsAbsoluteProgressForExactCountMission() {
        MissionDefinition definition = new MissionDefinition(
            "tuff-67",
            MissionScope.DAILY,
            "Toba exacta",
            "Consegui exactamente 67 de toba",
            MissionType.HOLD_EXACT_ITEM_COUNT,
            67,
            100.0,
            1,
            true,
            Map.of("material", "TUFF")
        );

        assertEquals(67, MissionProgressEngine.resolveAbsoluteProgress(definition, Map.of("count", 67)));
        assertTrue(MissionProgressEngine.isCompleted(definition, 67));
    }
}
