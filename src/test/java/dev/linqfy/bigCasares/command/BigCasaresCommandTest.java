package dev.linqfy.bigCasares.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BigCasaresCommandTest {

    @Test
    void recognizesReloadAndShopSubcommands() {
        assertTrue(BigCasaresCommand.isReload("reload"));
        assertTrue(BigCasaresCommand.isShop("shop"));
        assertTrue(BigCasaresCommand.isRating("rating"));
    }

    @Test
    void suggestsRootSubcommands() {
        assertEquals(9, BigCasaresCommand.rootSuggestions("").size());
        assertTrue(BigCasaresCommand.rootSuggestions("re").contains("reload"));
        assertTrue(BigCasaresCommand.rootSuggestions("ra").contains("rating"));
        assertTrue(BigCasaresCommand.rootSuggestions("te").contains("team"));
        assertTrue(BigCasaresCommand.rootSuggestions("ne").contains("nexus"));
        assertTrue(BigCasaresCommand.rootSuggestions("bo").contains("boss"));
    }

    @Test
    void suggestsCompleteTeamLifecycleAndColors() {
        assertEquals(
            java.util.List.of("create", "invite", "join", "leave", "kick", "dissolve", "rename", "tag", "color", "appearance"),
            BigCasaresCommand.teamSubcommandSuggestions("")
        );
        assertEquals(
            java.util.List.of("DARK_AQUA", "DARK_BLUE", "DARK_GRAY", "DARK_GREEN", "DARK_PURPLE", "DARK_RED"),
            BigCasaresCommand.teamColorSuggestions("dark_")
        );
    }
}
