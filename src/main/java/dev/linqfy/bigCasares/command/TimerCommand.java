package dev.linqfy.bigCasares.command;

import dev.linqfy.bigCasares.BigCasares;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class TimerCommand implements CommandExecutor, TabCompleter, Listener {
    private final BigCasares plugin;
    private BossBar activeTimerBar;
    private BukkitRunnable activeTimerTask;

    public TimerCommand(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bigcasares.command.timer")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /timer <seconds> [title...]");
            sender.sendMessage(ChatColor.RED + "Usage: /timer cancel");
            return true;
        }

        if (args[0].equalsIgnoreCase("cancel")) {
            if (activeTimerBar != null) {
                activeTimerBar.removeAll();
                activeTimerBar = null;
            }
            if (activeTimerTask != null) {
                activeTimerTask.cancel();
                activeTimerTask = null;
            }
            sender.sendMessage(ChatColor.GREEN + "Timer cancelled.");
            return true;
        }

        int totalSeconds;
        try {
            totalSeconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number of seconds. Must be an integer.");
            return true;
        }

        if (totalSeconds <= 0) {
            sender.sendMessage(ChatColor.RED + "Time must be greater than 0.");
            return true;
        }

        StringBuilder titleBuilder = new StringBuilder();
        if (args.length > 1) {
            for (int i = 1; i < args.length; i++) {
                titleBuilder.append(args[i]).append(" ");
            }
        } else {
            titleBuilder.append("Timer");
        }
        String baseTitle = ChatColor.translateAlternateColorCodes('&', titleBuilder.toString().trim());

        if (activeTimerBar != null) {
            activeTimerBar.removeAll();
        }
        if (activeTimerTask != null) {
            activeTimerTask.cancel();
        }

        activeTimerBar = Bukkit.createBossBar(formatTitle(baseTitle, totalSeconds), BarColor.BLUE, BarStyle.SOLID);
        for (Player p : Bukkit.getOnlinePlayers()) {
            activeTimerBar.addPlayer(p);
        }

        activeTimerTask = new BukkitRunnable() {
            int timeLeft = totalSeconds;

            @Override
            public void run() {
                timeLeft--;
                if (timeLeft <= 0) {
                    if (activeTimerBar != null) {
                        activeTimerBar.removeAll();
                        activeTimerBar = null;
                    }
                    activeTimerTask = null;
                    this.cancel();
                    return;
                }

                if (activeTimerBar != null) {
                    activeTimerBar.setTitle(formatTitle(baseTitle, timeLeft));
                    activeTimerBar.setProgress(Math.max(0.0, (double) timeLeft / totalSeconds));
                }
            }
        };
        activeTimerTask.runTaskTimer(plugin, 20L, 20L);
        sender.sendMessage(ChatColor.GREEN + "Timer started for " + totalSeconds + " seconds.");

        return true;
    }

    private String formatTitle(String base, int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        String timeStr;
        if (m > 0) {
            timeStr = String.format("%02d:%02d", m, s);
        } else {
            timeStr = String.format("%ds", s);
        }
        return base + " - " + timeStr;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("cancel".startsWith(args[0].toLowerCase())) {
                completions.add("cancel");
            }
            completions.add("60");
            completions.add("300");
            completions.add("600");
        }
        return completions;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (activeTimerBar != null) {
            activeTimerBar.addPlayer(event.getPlayer());
        }
    }
}
