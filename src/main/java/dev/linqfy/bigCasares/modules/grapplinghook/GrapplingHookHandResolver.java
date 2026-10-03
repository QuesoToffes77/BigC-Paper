package dev.linqfy.bigCasares.modules.grapplinghook;

import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

/** Resolves only custom Grappling Hooks and honors the actual input hand. */
public final class GrapplingHookHandResolver {

    private GrapplingHookHandResolver() {
    }

    public static Optional<Selection> select(
        String mainHandItemId,
        String offHandItemId,
        EquipmentSlot eventHand
    ) {
        if (eventHand == EquipmentSlot.HAND) {
            return GrapplingHookTier.fromCatalogId(mainHandItemId)
                .map(tier -> new Selection(EquipmentSlot.HAND, tier));
        }
        if (eventHand == EquipmentSlot.OFF_HAND) {
            return GrapplingHookTier.fromCatalogId(offHandItemId)
                .map(tier -> new Selection(EquipmentSlot.OFF_HAND, tier));
        }
        Optional<GrapplingHookTier> main = GrapplingHookTier.fromCatalogId(mainHandItemId);
        if (main.isPresent()) {
            return Optional.of(new Selection(EquipmentSlot.HAND, main.get()));
        }
        return GrapplingHookTier.fromCatalogId(offHandItemId)
            .map(tier -> new Selection(EquipmentSlot.OFF_HAND, tier));
    }

    public record Selection(EquipmentSlot hand, GrapplingHookTier tier) {
    }
}
