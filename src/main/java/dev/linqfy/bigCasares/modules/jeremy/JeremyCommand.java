package dev.linqfy.bigCasares.modules.jeremy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class JeremyCommand implements CommandExecutor, TabCompleter {
    private final JeremyModule module;

    JeremyCommand(JeremyModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            if (!allowed(sender, "bigcasares.jeremy.admin")) {
                return true;
            }
            module.statusLines().forEach(sender::sendMessage);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> start(sender, args);
            case "stop" -> result(sender, "bigcasares.jeremy.stop", module.stop());
            case "cooldown" -> cooldown(sender, args);
            default -> {
                sender.sendMessage("§cUso: /jeremy <status|start [player]|stop|cooldown reset>");
                yield true;
            }
        };
    }

    private boolean start(CommandSender sender, String[] args) {
        if (!allowed(sender, "bigcasares.jeremy.start")) {
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = module.server().getPlayer(args[1]);
        } else {
            target = sender instanceof Player player ? player : null;
        }
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado. Desde consola usa /jeremy start <player>.");
            return true;
        }
        send(sender, module.start(target));
        return true;
    }

    private boolean cooldown(CommandSender sender, String[] args) {
        if (!allowed(sender, "bigcasares.jeremy.admin")) {
            return true;
        }
        if (args.length != 2 || !"reset".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§cUso: /jeremy cooldown reset");
            return true;
        }
        send(sender, module.resetCooldown());
        return true;
    }

    private boolean result(CommandSender sender, String permission, JeremyActionResult action) {
        if (!allowed(sender, permission)) {
            return true;
        }
        send(sender, action);
        return true;
    }

    private static void send(CommandSender sender, JeremyActionResult result) {
        sender.sendMessage((result.success() ? "§a" : "§c") + result.message());
    }

    private static boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission("bigcasares.jeremy.admin")) {
            return true;
        }
        sender.sendMessage("§cNo tenes permiso para usar este comando.");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matches(args[0], List.of("status", "start", "stop", "cooldown"));
        }
        if (args.length == 2 && "start".equalsIgnoreCase(args[0])) {
            return matches(args[1], module.server().getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args.length == 2 && "cooldown".equalsIgnoreCase(args[0])) {
            return matches(args[1], List.of("reset"));
        }
        return List.of();
    }

    private static List<String> matches(String prefix, List<String> values) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                matches.add(value);
            }
        }
        return List.copyOf(matches);
    }
}
