package dev.linqfy.bigCasares.modules.airdrop;

import java.util.Optional;

public enum AirdropQuality {
    COMMON("COMÚN", "§f", 0),
    RARE("RARO", "§9", 1),
    EPIC("ÉPICO", "§5", 2),
    LEGENDARY("LEGENDARIO", "§6", 3),
    GHISTIC("GHÍSTICO", "§d§l", 4);

    private final String displayName;
    private final String color;
    private final int rank;

    AirdropQuality(String displayName, String color, int rank) {
        this.displayName = displayName;
        this.color = color;
        this.rank = rank;
    }

    public String displayName() {
        return displayName;
    }

    public String color() {
        return color;
    }

    public int rank() {
        return rank;
    }

    public Optional<AirdropQuality> next() {
        return rank + 1 < values().length ? Optional.of(values()[rank + 1]) : Optional.empty();
    }
}
