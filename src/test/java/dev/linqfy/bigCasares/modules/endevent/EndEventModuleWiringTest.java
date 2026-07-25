package dev.linqfy.bigCasares.modules.endevent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndEventModuleWiringTest {

    @Test
    void moduleIdIsStable() {
        assertEquals("end-event-system", new EndEventModule(null).getId());
    }
}
