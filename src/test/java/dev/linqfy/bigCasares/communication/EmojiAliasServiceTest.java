package dev.linqfy.bigCasares.communication;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EmojiAliasServiceTest {

    @Test
    void replacesKnownExactTokensAndPreservesUnknownAliases() {
        EmojiAliasService service = new EmojiAliasService(Map.of(
            ":heart:", "❤",
            ":star:", "★"
        ));

        assertEquals("hola ❤ :unknown: ★!", service.replace("hola :heart: :unknown: :star:!"));
    }

    @Test
    void replacementValuesAreNotProcessedRecursively() {
        Map<String, String> aliases = new LinkedHashMap<>();
        aliases.put(":first:", ":second:");
        aliases.put(":second:", "final");

        assertEquals(":second: final", new EmojiAliasService(aliases).replace(":first: :second:"));
    }

    @Test
    void defaultsContainTheSixPublicAliases() {
        assertEquals("☠ ❤ ★ ⚔ 🔥 🛡", EmojiAliasService.defaults().replace(
            ":skull: :heart: :star: :swords: :fire: :shield:"
        ));
    }
}
