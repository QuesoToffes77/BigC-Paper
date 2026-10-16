package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosMinionArmorTest {

    @Test
    void eachWinUpgradesTheArmor() {
        assertEquals(DonPollosMinionArmor.Tier.NONE, DonPollosMinionArmor.tier(0));
        assertEquals(DonPollosMinionArmor.Tier.COBRE, DonPollosMinionArmor.tier(1));
        assertEquals(DonPollosMinionArmor.Tier.HIERRO, DonPollosMinionArmor.tier(2));
        assertEquals(DonPollosMinionArmor.Tier.DIAMANTE, DonPollosMinionArmor.tier(3));
        assertEquals(DonPollosMinionArmor.Tier.NETHERITE, DonPollosMinionArmor.tier(4));
        assertEquals(DonPollosMinionArmor.Tier.NETHERITE, DonPollosMinionArmor.tier(20));
        assertEquals("COPPER_CHESTPLATE", DonPollosMinionArmor.Tier.COBRE.piece("CHESTPLATE"));
    }

    @Test
    void afterNetheriteTheEnchantmentsGetBetter() {
        assertEquals(0, DonPollosMinionArmor.protection(4, false));
        assertEquals(1, DonPollosMinionArmor.protection(5, false));
        assertEquals(5, DonPollosMinionArmor.protection(9, false));
        assertEquals(5, DonPollosMinionArmor.protection(50, false));
        assertEquals(0, DonPollosMinionArmor.thorns(9, false));
        assertEquals(1, DonPollosMinionArmor.thorns(10, false));
        assertEquals(3, DonPollosMinionArmor.thorns(50, false));
    }

    @Test
    void colonelsAlreadyInNetheriteGetEnchantedFromTheSecondFight() {
        assertEquals(0, DonPollosMinionArmor.protection(0, true));
        assertEquals(1, DonPollosMinionArmor.protection(1, true));
        assertEquals(5, DonPollosMinionArmor.protection(5, true));
        assertEquals(1, DonPollosMinionArmor.thorns(6, true));
    }

    @Test
    void armoredMobsUseTheModelWithTheirArmor() {
        assertEquals("bigcasares_labubu_rojo", DonPollosMinionArmor.model("bigcasares_labubu_rojo", 0));
        assertEquals("bigcasares_labubu_rojo_cobre", DonPollosMinionArmor.model("bigcasares_labubu_rojo", 1));
        assertEquals("bigcasares_gallina_wsp_netherite", DonPollosMinionArmor.model("bigcasares_gallina_wsp", 7));
        // el Coronel no tiene version con armadura: su modelo es siempre el mismo
        assertEquals("bigcasares_coronel", DonPollosMinionArmor.model("bigcasares_coronel", 3));
        assertEquals(20, DonPollosMinionArmor.armoredModels().size());
        assertTrue(DonPollosMinionArmor.armoredModels().contains("bigcasares_labubu_azul_diamante"));
    }

    @Test
    void everyArmoredModelShipsWithThePlugin() {
        for (String model : DonPollosMinionArmor.armoredModels()) {
            assertTrue(DonPollosModelInstaller.allModels().contains(model), model);
        }
    }
}
