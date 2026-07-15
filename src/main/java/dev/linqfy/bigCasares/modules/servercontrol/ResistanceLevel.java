package dev.linqfy.bigCasares.modules.servercontrol;

public enum ResistanceLevel {
    OFF(-1),
    I(0),
    II(1);

    private final int amplifier;

    ResistanceLevel(int amplifier) {
        this.amplifier = amplifier;
    }

    public int amplifier() {
        return amplifier;
    }

    public ResistanceLevel next() {
        return switch (this) {
            case OFF -> I;
            case I -> II;
            case II -> OFF;
        };
    }
}
