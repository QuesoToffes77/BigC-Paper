package dev.linqfy.bigCasares.modules.discord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DiscordModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("discord-integration", new DiscordIntegrationModule(null, null, null, null).getId());
    }
}
