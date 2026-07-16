package dev.linqfy.bigCasares.items.catalog;

public record ItemFoodDefinition(int nutrition, float saturation, boolean canAlwaysEat) {

    public ItemFoodDefinition {
        if (nutrition < 0) {
            throw new IllegalArgumentException("nutrition must not be negative");
        }
        if (saturation < 0.0F || !Float.isFinite(saturation)) {
            throw new IllegalArgumentException("saturation must be a finite non-negative number");
        }
    }
}
