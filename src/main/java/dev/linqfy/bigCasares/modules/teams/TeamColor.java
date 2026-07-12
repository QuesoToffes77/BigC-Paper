package dev.linqfy.bigCasares.modules.teams;

import java.util.Locale;
import java.util.Objects;

public enum TeamColor {
    BLACK('0'),
    DARK_BLUE('1'),
    DARK_GREEN('2'),
    DARK_AQUA('3'),
    DARK_RED('4'),
    DARK_PURPLE('5'),
    GOLD('6'),
    GRAY('7'),
    DARK_GRAY('8'),
    BLUE('9'),
    GREEN('a'),
    AQUA('b'),
    RED('c'),
    LIGHT_PURPLE('d'),
    YELLOW('e'),
    WHITE('f');

    private final char legacyCode;

    TeamColor(char legacyCode) {
        this.legacyCode = legacyCode;
    }

    public char code() {
        return legacyCode;
    }

    public String legacyCode() {
        return "§" + legacyCode;
    }

    public static TeamColor parse(String value) {
        Objects.requireNonNull(value, "value");
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
