package dev.linqfy.bigCasares.modules.celular;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CelularServiceTest {

    private static final List<CelularVideo> THREE = List.of(
        new CelularVideo("video_1", "Uno", 10),
        new CelularVideo("video_2", "Dos", 20),
        new CelularVideo("video_3", "Tres", 30));

    private static CelularService service(boolean villageLoot) {
        return new CelularService(THREE, new CelularSettings(true, villageLoot));
    }

    @Test
    void fGoesToTheNextVideoAndWrapsAround() {
        CelularService service = service(true);
        assertEquals(1, service.next(0));
        assertEquals(2, service.next(1));
        assertEquals(0, service.next(2));
        assertEquals(1, service.navigate(0, false));
    }

    @Test
    void shiftFGoesBackAndWrapsAround() {
        CelularService service = service(true);
        assertEquals(1, service.previous(2));
        assertEquals(0, service.previous(1));
        assertEquals(2, service.previous(0));
        assertEquals(2, service.navigate(0, true));
    }

    @Test
    void onBedrockThePhoneDoesNothingAndCraftingSaysTheMessage() {
        assertTrue(CelularService.worksFor(false));
        assertFalse(CelularService.worksFor(true));
        assertEquals("Dale bobi, no tenes java?, bancatela pibe", CelularService.BEDROCK_CRAFT_MESSAGE);
        assertTrue(CelularService.HINT.contains("F siguiente"));
    }

    @Test
    void geyserGetsTheStillModelAndOneModelPerVideo() {
        assertEquals(List.of("celular:celular", "celular:video_1", "celular:video_2", "celular:video_3"),
            service(true).itemModels());
    }

    @Test
    void phonesFindTheirVideoByIdEvenAfterTheListChanges() {
        CelularService service = service(true);
        assertEquals(2, service.resolve("video_3", null));
        assertEquals(1, service.resolve("video_2", 0), "the id wins over an old stored position");
        CelularService reordered = new CelularService(List.of(THREE.get(2), THREE.get(0), THREE.get(1)),
            new CelularSettings(true, true));
        assertEquals(0, reordered.resolve("video_3", null));
        assertEquals("Tres", reordered.video(reordered.resolve("video_3", null)).title());
    }

    @Test
    void phonesFromBeforeTheIdUseTheirOldPositionAndRemovedVideosFallBack() {
        CelularService service = service(true);
        assertEquals(2, service.resolve(null, 2));
        assertEquals(0, service.resolve(null, 9));
        assertEquals(0, service.resolve(null, null));
        assertEquals(0, service.resolve("video_que_ya_no_existe", 2));
    }

    @Test
    void outOfRangeIndexesFallBackToTheFirstVideo() {
        CelularService service = service(true);
        assertEquals(0, service.clamp(-1));
        assertEquals(0, service.clamp(3));
        assertEquals("Uno", service.video(42).title());
        assertEquals(1, service.next(42));
    }

    @Test
    void actionBarShowsPositionAndTitle() {
        assertEquals("▶ 2/3  Dos", service(true).actionBar(1));
    }

    @Test
    void needsAtLeastOneVideo() {
        assertThrows(IllegalArgumentException.class, () -> new CelularService(List.of(), CelularSettings.defaults()));
    }

    @Test
    void recognisesEveryVillageChestAndNothingElse() {
        assertTrue(CelularService.isVillageChest("minecraft", "chests/village/village_plains_house"));
        assertTrue(CelularService.isVillageChest("minecraft", "chests/village/village_weaponsmith"));
        assertTrue(CelularService.isVillageChest("minecraft", "chests/village/village_temple"));
        assertFalse(CelularService.isVillageChest("minecraft", "chests/simple_dungeon"));
        assertFalse(CelularService.isVillageChest("minecraft", "chests/pillager_outpost"));
        assertFalse(CelularService.isVillageChest("otro", "chests/village/village_plains_house"));
    }

    @Test
    void onlyTheFirstChestOfEachVillageGetsAPhone() {
        CelularService service = service(true);
        assertTrue(service.shouldAddVillagePhone("minecraft", "chests/village/village_desert_house", true, false));
        assertFalse(service.shouldAddVillagePhone("minecraft", "chests/village/village_desert_house", true, true));
        assertFalse(service.shouldAddVillagePhone("minecraft", "chests/simple_dungeon", true, false));
    }

    @Test
    void aVillageLootTableOutsideARecognisedVillageNeverGivesAPhone() {
        CelularService service = service(true);
        assertFalse(service.shouldAddVillagePhone("minecraft", "chests/village/village_plains_house", false, false));
        assertFalse(service.shouldAddVillagePhone("minecraft", "chests/village/village_plains_house", false, true));
    }

    @Test
    void villageLootCanBeTurnedOff() {
        assertFalse(service(false).shouldAddVillagePhone("minecraft", "chests/village/village_plains_house", true, false));
    }
}
