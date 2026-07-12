package dev.linqfy.bigCasares.modules.teams;

import java.util.Locale;

public enum TeamTagWrapper {
    SQUARE("[", "]"),
    ANGLE("<", ">"),
    ROUND("(", ")"),
    FULLWIDTH("【", "】"),
    NONE("", "");

    private final String opening;
    private final String closing;

    TeamTagWrapper(String opening, String closing) {
        this.opening = opening;
        this.closing = closing;
    }

    public String wrap(String tag) {
        return opening + tag + closing;
    }

    public static TeamTagWrapper parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
