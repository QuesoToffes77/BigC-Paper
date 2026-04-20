package dev.linqfy.bigCasares.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BigCasaresCommandTest {

    @Test
    void recognizesReloadAndShopSubcommands() {
        assertTrue(BigCasaresCommand.isReload("reload"));
        assertTrue(BigCasaresCommand.isShop("shop"));
    }

    @Test
    void suggestsRootSubcommands() {
        assertEquals(4, BigCasaresCommand.rootSuggestions("").size());
        assertTrue(BigCasaresCommand.rootSuggestions("re").contains("reload"));
    }
}
