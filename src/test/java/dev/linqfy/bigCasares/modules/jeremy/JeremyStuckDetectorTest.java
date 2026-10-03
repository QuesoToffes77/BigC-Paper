package dev.linqfy.bigCasares.modules.jeremy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeremyStuckDetectorTest {

    @Test
    void jeremyDoesNotUseUltrasoundWhileMakingProgress() {
        JeremyStuckDetector detector = new JeremyStuckDetector(80, 1.5, 4.0);
        detector.sample(0, new JeremyPosition(0, 0, 0), 20, true);

        assertFalse(detector.sample(40, new JeremyPosition(2, 0, 0), 18, true));
        assertFalse(detector.sample(100, new JeremyPosition(4, 0, 0), 16, true));
    }

    @Test
    void jeremyBecomesStuckAfterConfiguredNoProgressWindow() {
        JeremyStuckDetector detector = new JeremyStuckDetector(80, 1.5, 4.0);
        detector.sample(0, new JeremyPosition(0, 0, 0), 20, true);

        assertFalse(detector.sample(79, new JeremyPosition(0.2, 0, 0), 19.8, true));
        assertTrue(detector.sample(80, new JeremyPosition(0.2, 0, 0), 19.8, true));
    }

    @Test
    void progressClearsStuckState() {
        JeremyStuckDetector detector = new JeremyStuckDetector(80, 1.5, 4.0);
        detector.sample(0, new JeremyPosition(0, 0, 0), 20, true);
        assertTrue(detector.sample(80, new JeremyPosition(0, 0, 0), 20, true));

        assertFalse(detector.sample(100, new JeremyPosition(2, 0, 0), 18, true));
    }
}
