package dev.linqfy.bigCasares.modules.glider;

import java.util.Optional;

public final class GliderRecipePolicy {

    private GliderRecipePolicy() {
    }

    public static Optional<GliderTier> previousTier(GliderTier target) {
        return GliderTier.fromNumber(target.number() - 1);
    }

    public static boolean acceptsUpgradeIngredient(GliderTier target, String catalogItemId) {
        return previousTier(target)
            .map(GliderTier::catalogId)
            .map(expected -> expected.equals(catalogItemId))
            .orElse(false);
    }
}
