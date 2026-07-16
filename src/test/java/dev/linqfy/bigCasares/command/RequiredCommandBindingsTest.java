package dev.linqfy.bigCasares.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequiredCommandBindingsTest {

    @Test
    void resolvesEveryRequiredCommandBeforeReturningBindings() {
        Map<String, String> commands = Map.of("shop", "shop-binding", "rating", "rating-binding");

        List<String> resolved = RequiredCommandBindings.resolve(commands::get, List.of("shop", "rating"));

        assertEquals(List.of("shop-binding", "rating-binding"), resolved);
    }

    @Test
    void rejectsMissingRequiredCommandByName() {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
            () -> RequiredCommandBindings.resolve(name -> null, List.of("shop")));

        assertEquals("Required command is not declared: shop", failure.getMessage());
    }
}
