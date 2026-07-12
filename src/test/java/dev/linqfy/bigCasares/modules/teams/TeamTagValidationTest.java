package dev.linqfy.bigCasares.modules.teams;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamTagValidationTest {

    @Test
    void acceptsOneToSixSafeVisibleUnicodeGraphemes() {
        assertTrue(TeamTagValidator.isValid("A"));
        assertTrue(TeamTagValidator.isValid("AB"));
        assertTrue(TeamTagValidator.isValid("A1B2C"));
        assertTrue(TeamTagValidator.isValid("ABCDEF"));

        assertFalse(TeamTagValidator.isValid("ABCDEFG"));
        assertTrue(TeamTagValidator.isValid("abc"));
        assertTrue(TeamTagValidator.isValid("⚔★"));
        assertFalse(TeamTagValidator.isValid("A§c"));
        assertFalse(TeamTagValidator.isValid("A\u200BB"));
        assertFalse(TeamTagValidator.isValid(""));
        assertFalse(TeamTagValidator.isValid(null));
    }

    @Test
    void creationNormalizesInputAndRejectsDuplicateTags() {
        TeamService service = new TeamService(new InMemoryTeamStorage());
        UUID firstOwner = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondOwner = UUID.fromString("00000000-0000-0000-0000-000000000002");

        Team created = service.createTeam(
            new TeamId(UUID.fromString("10000000-0000-0000-0000-000000000001")),
            "Guardianes del Sur",
            "gds",
            TeamColor.DARK_AQUA,
            firstOwner,
            Instant.parse("2026-07-11T12:00:00Z")
        );

        assertEquals("GDS", created.tag());
        assertThrows(
            IllegalArgumentException.class,
            () -> service.createTeam(
                new TeamId(UUID.fromString("10000000-0000-0000-0000-000000000002")),
                "Guardianes del Norte",
                "GDS",
                TeamColor.BLUE,
                secondOwner,
                Instant.parse("2026-07-11T12:01:00Z")
            )
        );
    }
}
