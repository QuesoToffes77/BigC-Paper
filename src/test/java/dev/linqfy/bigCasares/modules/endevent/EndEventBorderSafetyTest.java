package dev.linqfy.bigCasares.modules.endevent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndEventBorderSafetyTest {

    @Test
    void projectsCoordinatesInsideEveryMovingBorderEdge() {
        EndEventBorderPoint projected = EndEventBorderSafety.projectInside(
            100.0, -50.0, 20.0, 3.0, 500.0, -500.0
        );

        assertEquals(107.0, projected.x());
        assertEquals(-57.0, projected.z());
        assertTrue(EndEventBorderSafety.isInsideInset(
            100.0, -50.0, 20.0, 3.0, projected.x(), projected.z()
        ));
    }

    @Test
    void leavesAlreadySafeCoordinatesUnchanged() {
        assertEquals(
            new EndEventBorderPoint(95.5, -47.25),
            EndEventBorderSafety.projectInside(100.0, -50.0, 100.0, 3.0, 95.5, -47.25)
        );
    }

    @Test
    void handlesMarginsLargerThanTinyBorderWithoutInvertingBounds() {
        EndEventBorderPoint point = EndEventBorderSafety.projectInside(
            8.0, 12.0, 4.0, 3.0, 999.0, 999.0
        );

        assertEquals(new EndEventBorderPoint(8.0, 12.0), point);
    }
}
