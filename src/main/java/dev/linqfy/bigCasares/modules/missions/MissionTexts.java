package dev.linqfy.bigCasares.modules.missions;

import org.bukkit.ChatColor;

import java.time.Duration;

public final class MissionTexts {

    private MissionTexts() {
    }

    public static String title(MissionHudView view) {
        return switch (view) {
            case MAIN_MENU -> ChatColor.DARK_GREEN + "Misiones";
            case DAILY_LIST -> ChatColor.GOLD + "Misiones diarias";
            case WEEKLY_LIST -> ChatColor.AQUA + "Misiones semanales";
            case CLAIMABLES -> ChatColor.GREEN + "Recompensas listas";
        };
    }

    public static String status(MissionAssignment assignment) {
        if (assignment.snapshot().claimed()) {
            return ChatColor.GREEN + "Reclamada";
        }
        if (assignment.snapshot().completed()) {
            return ChatColor.GOLD + "Completada";
        }
        return ChatColor.GRAY + "Pendiente";
    }

    public static String formatTimeRemaining(Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}
