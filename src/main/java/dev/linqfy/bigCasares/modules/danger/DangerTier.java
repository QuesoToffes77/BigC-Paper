package dev.linqfy.bigCasares.modules.danger;

public enum DangerTier {
    NORMAL(1, 0.0, 0xAAAAAA, "Normal"),
    PELIGROSO(2, 45.0, 0xFFFF55, "Peligroso"),
    LETAL(3, 80.0, 0xFF5555, "Letal");

    private final int level;
    private final double minimum;
    private final int colorRgb;
    private final String displayName;

    DangerTier(int level, double minimum, int colorRgb, String displayName) {
        this.level = level;
        this.minimum = minimum;
        this.colorRgb = colorRgb;
        this.displayName = displayName;
    }

    public int level() { return level; }
    public double minimum() { return minimum; }
    public int colorRgb() { return colorRgb; }
    public String displayName() { return displayName; }

    public static DangerTier fromScore(double score) {
        if (score >= LETAL.minimum) return LETAL;
        if (score >= PELIGROSO.minimum) return PELIGROSO;
        return NORMAL;
    }

    public static DangerTier fromLevel(int level) {
        return level >= 3 ? LETAL : level == 2 ? PELIGROSO : NORMAL;
    }
}
