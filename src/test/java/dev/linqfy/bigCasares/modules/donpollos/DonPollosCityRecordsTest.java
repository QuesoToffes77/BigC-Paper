package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosCityRecordsTest {

    private static final String WORLD = "3f0a2b1c-0000-4000-8000-000000000001";

    @Test
    void seededKeysGiveTheirCell() {
        String key = WORLD + ";-2;7";
        assertFalse(DonPollosCityRecords.isManual(key));
        assertArrayEquals(new int[]{-2, 7}, DonPollosCityRecords.seededCell(key).orElseThrow());
        assertEquals(WORLD, DonPollosCityRecords.worldId(key));
    }

    @Test
    void pendingManualCityKeyIsRecognisedInsteadOfCrashing() {
        // antes: Integer.parseInt("manual") tiraba NumberFormatException y el modulo no arrancaba nunca mas
        String key = WORLD + ";manual;1760000000000";
        assertTrue(DonPollosCityRecords.isManual(key));
        assertTrue(DonPollosCityRecords.seededCell(key).isEmpty());
        assertEquals(WORLD, DonPollosCityRecords.worldId(key));
    }

    @Test
    void brokenKeysAreSkippedWithoutThrowing() {
        assertTrue(DonPollosCityRecords.seededCell(WORLD + ";x;1").isEmpty());
        assertTrue(DonPollosCityRecords.seededCell(WORLD + ";1").isEmpty());
        assertTrue(DonPollosCityRecords.seededCell("").isEmpty());
        assertFalse(DonPollosCityRecords.isManual(WORLD + ";manual"));
    }

    @Test
    void manualPlanSurvivesSavingAndLoading() {
        DonPollosCityLayout.CityPlan plan =
            new DonPollosCityLayout.CityPlan(Integer.MIN_VALUE, Integer.MIN_VALUE, -120, 4500, 3, 56, 45);

        Optional<DonPollosCityLayout.CityPlan> loaded =
            DonPollosCityRecords.decodePlan(DonPollosCityRecords.encodePlan(plan));

        assertEquals(Optional.of(plan), loaded);
    }

    @Test
    void invalidManualPlansAreRejected() {
        assertTrue(DonPollosCityRecords.decodePlan("1,2,3").isEmpty());
        assertTrue(DonPollosCityRecords.decodePlan("a,2,0,45,56").isEmpty());
        assertTrue(DonPollosCityRecords.decodePlan("1,2,4,45,56").isEmpty());
        assertTrue(DonPollosCityRecords.decodePlan("1,2,0,0,56").isEmpty());
    }
}
