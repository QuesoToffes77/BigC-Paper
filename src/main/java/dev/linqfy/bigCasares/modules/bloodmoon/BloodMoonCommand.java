package dev.linqfy.bigCasares.modules.bloodmoon;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

final class BloodMoonCommand implements CommandExecutor, TabCompleter {
    private final BloodMoonModule module;

    BloodMoonCommand(BloodMoonModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bigcasares.bloodmoon.admin")) {
            sender.sendMessage("§cNo tienes permiso.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            module.statusLines().forEach(sender::sendMessage);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> start(sender, args);
            case "stop" -> stop(sender, args);
            default -> {
                sender.sendMessage("§cUso: /bloodmoon <start|stop|status> [mundo|all]");
                yield true;
            }
        };
    }

    private boolean start(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bigcasares.bloodmoon.start")) {
            sender.sendMessage("§cNo tienes permiso para iniciar Blood Moon.");
            return true;
        }
        World world = resolveWorld(sender, args.length >= 2 ? args[1] : null);
        BloodMoonActionResult result = module.start(world);
        sender.sendMessage((result.success() ? "§a" : "§c") + result.message());
        return true;
    }

    private boolean stop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bigcasares.bloodmoon.stop")) {
            sender.sendMessage("§cNo tienes permiso para detener Blood Moon.");
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
            sender.sendMessage("§aBlood Moons detenidas: " + module.stopAll());
            return true;
        }
        World world = resolveWorld(sender, args.length >= 2 ? args[1] : null);
        BloodMoonActionResult result = module.stop(world);
        sender.sendMessage((result.success() ? "§a" : "§c") + result.message());
        return true;
    }

    private World resolveWorld(CommandSender sender, String configured) {
        if (configured != null && !configured.isBlank()) {
            return module.server().getWorld(configured);
        }
        return sender instanceof Player player ? player.getWorld() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return List.of("start", "stop", "status").stream().filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("stop"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            java.util.ArrayList<String> worlds = new java.util.ArrayList<>(module.server().getWorlds().stream()
                .map(World::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList());
            if (args[0].equalsIgnoreCase("stop") && "all".startsWith(prefix)) {
                worlds.add("all");
            }
            return List.copyOf(worlds);
        }
        return List.of();
    }
}
