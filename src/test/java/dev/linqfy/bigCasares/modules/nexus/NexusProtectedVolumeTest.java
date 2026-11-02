package dev.linqfy.bigCasares.modules.nexus;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NexusProtectedVolumeTest {

    @Test
    void aBoxTouchingTheNexusVolumeIsProtected() {
        // nexus en (100, 64, 100) con 8 de radio horizontal y 4 vertical: x/z 92..108, y 60..68
        assertTrue(NexusModule.volumeTouches(100, 64, 100, 8, 4, 96, 111, 55, 62, 96, 111));
        assertTrue(NexusModule.volumeTouches(100, 64, 100, 8, 4, 108, 123, 60, 68, 100, 100), "borde justo");
        assertFalse(NexusModule.volumeTouches(100, 64, 100, 8, 4, 109, 124, 60, 68, 100, 100), "un bloque afuera");
        assertFalse(NexusModule.volumeTouches(100, 64, 100, 8, 4, 96, 111, 69, 80, 96, 111), "arriba del volumen");
    }

    @Test
    void withTheModuleOffNothingIsProtected() {
        assertFalse(new NexusModule(null).intersectsProtectedVolume(UUID.randomUUID(), 0, 15, 0, 100, 0, 15));
    }
}
