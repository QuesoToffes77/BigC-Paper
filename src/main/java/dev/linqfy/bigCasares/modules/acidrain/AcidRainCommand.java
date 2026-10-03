package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AcidRainCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN = "bigcasares.acidrain.admin";
    private static final String START = "bigcasares.acidrain.start";
    private static final String STOP = "bigcasares.acidrain.stop";
    private static final String RELOAD = "bigcasares.acidrain.reload";

    private final AcidRainModule module;

    AcidRainCommand(AcidRainModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            if (!has(sender, ADMIN)) {
                deny(sender);
                return true;
            }
            module.infoLines().forEach(sender::sendMessage);
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "start" -> start(sender, args);
            case "stop" -> stop(sender);
            case "reload" -> reload(sender);
            default -> usage(sender, label);
        }
        return true;
    }

    private void start(CommandSender sender, String[] args) {
        if (!has(sender, START)) {
            deny(sender);
            return;
        }
        AcidRainLevel level = args.length >= 2
            ? AcidRainLevel.parse(args[1]).orElse(null)
            : null;
        if (args.length >= 2 && level == null) {
            sender.sendMessage(ChatColor.RED + "Nivel invalido. Usa acid, toxic o chemical.");
            return;
        }
        AcidRainStartResult result = module.start(level);
        if (result.started()) {
            sender.sendMessage(ChatColor.GREEN + "Acid Rain iniciada en nivel " + result.level().name() + ".");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "No se pudo iniciar: " + result.message());
        }
    }

    private void stop(CommandSender sender) {
        if (!has(sender, STOP)) {
            deny(sender);
            return;
        }
        AcidRainTransitionResult result = module.stop("command");
        if (result.transitioned()) {
            sender.sendMessage(ChatColor.GREEN + "Acid Rain detenida y recursos temporales limpiados.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + result.message());
        }
    }

    private void reload(CommandSender sender) {
        if (!has(sender, RELOAD)) {
            deny(sender);
            return;
        }
        AcidRainConfigLoadResult result = module.reloadSettings();
        if (result.valid()) {
            sender.sendMessage(ChatColor.GREEN + "Configuracion de Acid Rain recargada.");
            if (!result.warnings().isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Advertencias: " + result.warnings().size());
            }
            return;
        }
        sender.sendMessage(ChatColor.RED + "Configuracion invalida. Se mantiene la configuracion segura anterior.");
        result.errors().forEach(error -> sender.sendMessage(ChatColor.RED + "- " + error));
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission(ADMIN);
    }

    private void deny(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "No tenes permiso.");
    }

    private void usage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " <start|stop|reload|info> [acid|toxic|chemical]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> candidates = new ArrayList<>();
        if (args.length == 1) {
            candidates.addAll(List.of("start", "stop", "reload", "info"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            candidates.addAll(List.of("acid", "toxic", "chemical"));
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(value -> value.startsWith(prefix)).sorted().toList();
    }
}
