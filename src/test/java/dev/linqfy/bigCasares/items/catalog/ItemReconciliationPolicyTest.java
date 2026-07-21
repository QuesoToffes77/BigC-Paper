package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemReconciliationPolicyTest {

    @Test
    void refreshesDefinitionsThatRemainInTheCandidateCatalog() {
        CustomItemCatalog previous = new CustomItemCatalog(Map.of("copper_apple", definition("copper_apple")));
        CustomItemCatalog candidate = new CustomItemCatalog(Map.of("copper_apple", definition("copper_apple")));

        assertEquals(ItemReconciliationDecision.REFRESH,
            new ItemReconciliationPolicy().decide("copper_apple", previous, candidate));
    }

    @Test
    void marksRemovedDefinitionsLegacyAndLeavesUnknownStacksUntouched() {
        CustomItemCatalog previous = new CustomItemCatalog(Map.of("copper_apple", definition("copper_apple")));
        CustomItemCatalog candidate = new CustomItemCatalog(Map.of("smoke_bomb", definition("smoke_bomb")));
        ItemReconciliationPolicy policy = new ItemReconciliationPolicy();

        assertEquals(ItemReconciliationDecision.MARK_LEGACY,
            policy.decide("copper_apple", previous, candidate));
        assertEquals(ItemReconciliationDecision.IGNORE,
            policy.decide("unrelated_item", previous, candidate));
    }

    private static CustomItemDefinition definition(String id) {
        String mechanic = "copper_apple".equals(id) ? "copper-apple" : "smoke-bomb";
        String material = "copper_apple".equals(id) ? "APPLE" : "SNOWBALL";
        return new CustomItemDefinition(
            id, mechanic, material, "bigcasares:" + id, OptionalInt.empty(),
            new ItemDisplayDefinition("item.bigcasares." + id, id, List.of()), 64, null, null, null, null,
            new ItemAppearanceDefinition(
                "java/assets/bigcasares/items/" + id + ".json",
                "bedrock/textures/item/" + id + ".png"
            )
        );
    }
}
