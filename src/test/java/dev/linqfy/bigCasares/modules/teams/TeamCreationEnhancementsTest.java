package dev.linqfy.bigCasares.modules.teams;

import dev.linqfy.bigCasares.command.BigCasaresCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeamCreationEnhancementsTest {

    @Test
    void parsesSimpleArguments() {
        String[] args = {"create", "Casares", "LHDC", "RED"};
        List<String> parsed = BigCasaresCommand.parseQuotedArgs(args);
        assertEquals(List.of("create", "Casares", "LHDC", "RED"), parsed);
    }

    @Test
    void parsesQuotedArgumentsWithSpaces() {
        String[] args = {"create", "\"Los", "Heraldos", "De", "Casares\"", "LHDC", "RED"};
        List<String> parsed = BigCasaresCommand.parseQuotedArgs(args);
        assertEquals(List.of("create", "Los Heraldos De Casares", "LHDC", "RED"), parsed);
    }

    @Test
    void parsesQuotedArgumentsSingleWord() {
        String[] args = {"create", "\"Casares\"", "LHDC", "RED"};
        List<String> parsed = BigCasaresCommand.parseQuotedArgs(args);
        assertEquals(List.of("create", "Casares", "LHDC", "RED"), parsed);
    }
}
