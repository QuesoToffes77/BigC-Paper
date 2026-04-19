package dev.linqfy.bigCasares.modules.missions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MissionCommandTest {

    @Test
    void parsesDailyShortcut() {
        assertEquals(MissionHudView.DAILY_LIST, MissionCommand.resolveView(new String[]{"misiones", "diarias"}));
    }

    @Test
    void defaultsToMainMenu() {
        assertEquals(MissionHudView.MAIN_MENU, MissionCommand.resolveView(new String[]{"misiones"}));
    }
}
