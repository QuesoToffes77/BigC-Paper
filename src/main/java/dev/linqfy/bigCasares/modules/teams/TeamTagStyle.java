package dev.linqfy.bigCasares.modules.teams;

import java.util.Objects;

public record TeamTagStyle(
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        TeamTagWrapper wrapper
) {
    public static final TeamTagStyle DEFAULT = new TeamTagStyle(true, false, false, false, TeamTagWrapper.SQUARE);

    public TeamTagStyle {
        Objects.requireNonNull(wrapper, "wrapper");
    }

    public String legacyDecorations() {
        StringBuilder result = new StringBuilder();
        if (bold) result.append("§l");
        if (italic) result.append("§o");
        if (underlined) result.append("§n");
        if (strikethrough) result.append("§m");
        return result.toString();
    }

    public TeamTagStyle withBold(boolean value) { return new TeamTagStyle(value, italic, underlined, strikethrough, wrapper); }
    public TeamTagStyle withItalic(boolean value) { return new TeamTagStyle(bold, value, underlined, strikethrough, wrapper); }
    public TeamTagStyle withUnderlined(boolean value) { return new TeamTagStyle(bold, italic, value, strikethrough, wrapper); }
    public TeamTagStyle withStrikethrough(boolean value) { return new TeamTagStyle(bold, italic, underlined, value, wrapper); }
    public TeamTagStyle withWrapper(TeamTagWrapper value) { return new TeamTagStyle(bold, italic, underlined, strikethrough, value); }
}
