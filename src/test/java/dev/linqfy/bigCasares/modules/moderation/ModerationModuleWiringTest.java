package dev.linqfy.bigCasares.modules.moderation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ModerationModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("moderation-system", new ModerationModule(null).getId());
    }
}
