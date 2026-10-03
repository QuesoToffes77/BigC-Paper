package dev.linqfy.bigCasares.modules.airdrop;

import dev.linqfy.bigCasares.items.CustomItemRegistry;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

final class AirdropCustomItemValidator {
    private AirdropCustomItemValidator() {
    }

    static void validate(CustomItemRegistry registry) {
        validate(id -> registry.findById(id).isPresent());
    }

    static void validate(Predicate<String> exists) {
        for (String id : configuredIds()) {
            if (!exists.test(id)) {
                throw new IllegalStateException("Unknown Airdrop custom item factory: " + id);
            }
        }
    }

    static Set<String> configuredIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (AirdropQuality quality : AirdropQuality.values()) {
            AirdropLootTable.customItems(quality).forEach(entry -> ids.add(entry.itemId()));
        }
        return Set.copyOf(ids);
    }
}
