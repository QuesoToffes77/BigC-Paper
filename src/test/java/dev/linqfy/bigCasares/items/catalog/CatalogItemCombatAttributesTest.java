package dev.linqfy.bigCasares.items.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogItemCombatAttributesTest {

    @Test
    void plansStableModifiersThatProduceTheExactFinalCombatValues() {
        ItemCombatDefinition combat = new ItemCombatDefinition(6.0, 3.2);

        CatalogItemStackFactory.CombatModifierPlan first =
            CatalogItemStackFactory.combatModifierPlan("sahurs_bat", combat);
        CatalogItemStackFactory.CombatModifierPlan second =
            CatalogItemStackFactory.combatModifierPlan("sahurs_bat", combat);

        assertEquals(first, second);
        assertEquals("sahurs_bat_attack_damage", first.damageKey());
        assertEquals(5.0, first.damageAmount());
        assertEquals("sahurs_bat_attack_speed", first.speedKey());
        assertEquals(-0.8, first.speedAmount(), 0.0000001);
    }
}
