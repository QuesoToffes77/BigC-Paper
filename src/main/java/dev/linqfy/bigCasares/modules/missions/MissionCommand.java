package dev.linqfy.bigCasares.modules.missions;

import java.util.Locale;

public final class MissionCommand {

    private MissionCommand() {
    }

    public static MissionHudView resolveView(String[] args) {
        if (args.length < 2) {
            return MissionHudView.MAIN_MENU;
        }

        return switch (args[1].toLowerCase(Locale.ROOT)) {
            case "diarias", "diaria", "daily" -> MissionHudView.DAILY_LIST;
            case "semanales", "semanal", "weekly" -> MissionHudView.WEEKLY_LIST;
            case "reclamar", "claim" -> MissionHudView.CLAIMABLES;
            default -> MissionHudView.MAIN_MENU;
        };
    }
}
