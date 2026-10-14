package dev.linqfy.bigCasares.modules.cosmicniv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What an entity wears ({@code armor}) and the items it holds or carries ({@code held}). The model takes the best
 * weapon and the best tool among {@code held}; held items that are neither are ignored.
 */
public record CosmicNivLoadout(Map<CosmicNivArmorSlot, CosmicNivItem> armor, List<CosmicNivItem> held) {
    private static final CosmicNivLoadout EMPTY = new CosmicNivLoadout(Map.of(), List.of());

    public CosmicNivLoadout {
        Objects.requireNonNull(armor, "armor");
        Objects.requireNonNull(held, "held");
        EnumMap<CosmicNivArmorSlot, CosmicNivItem> copy = new EnumMap<>(CosmicNivArmorSlot.class);
        armor.forEach((slot, item) -> {
            if (slot != null && item != null) {
                copy.put(slot, item);
            }
        });
        armor = Collections.unmodifiableMap(copy);
        held = held.stream().filter(Objects::nonNull).toList();
    }

    public static CosmicNivLoadout empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final EnumMap<CosmicNivArmorSlot, CosmicNivItem> armor = new EnumMap<>(CosmicNivArmorSlot.class);
        private final List<CosmicNivItem> held = new ArrayList<>();

        private Builder() {
        }

        public Builder armor(CosmicNivArmorSlot slot, CosmicNivItem item) {
            armor.put(Objects.requireNonNull(slot, "slot"), Objects.requireNonNull(item, "item"));
            return this;
        }

        /**
         * Adds a held or carried item (weapon, tool or anything else).
         */
        public Builder held(CosmicNivItem item) {
            held.add(Objects.requireNonNull(item, "item"));
            return this;
        }

        public CosmicNivLoadout build() {
            return new CosmicNivLoadout(armor, held);
        }
    }
}
