package dev.linqfy.bigCasares.modules.glider;

import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

/** Deterministic hand selection shared by activation, physics and boost input. */
public final class GliderHandResolver {

    private GliderHandResolver() {
    }

    public static Optional<Selection> select(String mainHandItemId, String offHandItemId) {
        Optional<GliderTier> main = GliderTier.fromCatalogId(mainHandItemId);
        Optional<GliderTier> off = GliderTier.fromCatalogId(offHandItemId);
        if (main.isEmpty()) {
            return off.map(tier -> new Selection(EquipmentSlot.OFF_HAND, tier));
        }
        if (off.isEmpty() || main.get().number() >= off.get().number()) {
            return Optional.of(new Selection(EquipmentSlot.HAND, main.get()));
        }
        return Optional.of(new Selection(EquipmentSlot.OFF_HAND, off.get()));
    }

    public record Selection(EquipmentSlot hand, GliderTier tier) {
    }
}
