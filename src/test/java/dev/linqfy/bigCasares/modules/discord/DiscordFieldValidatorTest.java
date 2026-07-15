package dev.linqfy.bigCasares.modules.discord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DiscordFieldValidatorTest {

    @Test
    void acceptsAndNormalizesPresetFields() {
        assertEquals("hola con espacios", DiscordFieldValidator.validate("servidor.description", "\"hola con espacios\""));
        assertEquals("#12ABEF", DiscordFieldValidator.validate("jugadores.color", "#12abef"));
        assertEquals("25565", DiscordFieldValidator.validate("servidor.port", "25565"));
        assertEquals("", DiscordFieldValidator.validate("servidor.image", ""));
    }

    @Test
    void rejectsUnknownFieldsUnsafeUrlsAndInvalidLimits() {
        assertThrows(IllegalArgumentException.class, () -> DiscordFieldValidator.validate("servidor.token", "secret"));
        assertThrows(IllegalArgumentException.class, () -> DiscordFieldValidator.validate("servidor.image", "file:///tmp/a"));
        assertThrows(IllegalArgumentException.class, () -> DiscordFieldValidator.validate("servidor.port", "70000"));
        assertThrows(IllegalArgumentException.class, () -> DiscordFieldValidator.validate("jugadores.title", "x".repeat(257)));
    }
}
