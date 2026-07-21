package dev.linqfy.bigCasares.items.catalog;

public record ItemCombatDefinition(double attackDamage, double attackSpeed) {

    public ItemCombatDefinition {
        if (!Double.isFinite(attackDamage) || attackDamage < 0.0) {
            throw new IllegalArgumentException("attackDamage must be finite and non-negative");
        }
        if (!Double.isFinite(attackSpeed) || attackSpeed <= 0.0) {
            throw new IllegalArgumentException("attackSpeed must be finite and positive");
        }
    }
}
