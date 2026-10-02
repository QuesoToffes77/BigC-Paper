package dev.linqfy.bigCasares.modules.airdrop;

public enum AirdropType {
    HE("Alta Explosividad", "§c"),
    LUXURY("Lujo", "§b"),
    ENCHANT("Encantamientos", "§d"),
    RARE_ITEMS("Objetos Raros", "§e");

    private final String displayName;
    private final String color;

    AirdropType(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public String color() {
        return color;
    }
}
