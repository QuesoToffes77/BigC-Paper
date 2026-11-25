package dev.linqfy.bigCasares.modules.toffy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ToffyCommand implements CommandExecutor, TabCompleter {
    static final String CHECK_PERMISSION = "bigcasares.toffy.check";
    static final String CHECK_OTHERS_PERMISSION = "bigcasares.toffy.check.others";
    static final String ADMIN_PERMISSION = "bigcasares.toffy.admin";
    static final String TORNEO_ADMIN_PERMISSION = ToffyTournamentCommand.PERMISSION;
    private static final String PREFIX = "§6Toffy §8» §7";
    private static final int MAX_ERRORS_SHOWN = 5;

    private final ToffyModule module;
    private final ToffyTournamentCommand tournamentCommand;

    ToffyCommand(ToffyModule module) {
        this.module = module;
        this.tournamentCommand = new ToffyTournamentCommand(module);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!module.isAvailable()) {
            sender.sendMessage("§cToffy Complementaries no esta disponible.");
            return true;
        }
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cDesde consola usa /toffy <jugador>.");
                return true;
            }
            if (allowed(sender, CHECK_PERMISSION)) {
                show(sender, player);
            }
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (allowed(sender, ADMIN_PERMISSION)) {
                    reload(sender);
                }
            }
            case "torneo", "tp" -> tournamentCommand.execute(sender, args);
            case "curva", "curve" -> {
                if (allowed(sender, ADMIN_PERMISSION)) {
                    curve(sender);
                }
            }
            default -> {
                boolean self = sender instanceof Player player && player.getName().equalsIgnoreCase(args[0]);
                if (!allowed(sender, self ? CHECK_PERMISSION : CHECK_OTHERS_PERMISSION)) {
                    return true;
                }
                Player target = module.server().getPlayerExact(args[0]);
                if (target == null) {
                    sender.sendMessage("§cJugador no encontrado: " + args[0]);
                    return true;
                }
                show(sender, target);
            }
        }
        return true;
    }

    private void show(CommandSender sender, Player target) {
        ToffyResult result = module.evaluate(target);
        int decimals = module.service().settings().decimals();
        sender.sendMessage(PREFIX + "§f" + target.getName() + "§7: §e" + level(result.value(), decimals)
            + "§7/§e" + level(result.maximum(), decimals));
        sender.sendMessage("§7  Armadura: §f" + percent(result.armorScore())
            + " §8(poder " + format(result.armorPower()) + ")");
        sender.sendMessage(heldLine("Arma", result.weapon()));
        if (module.service().settings().toolWeight() > 0.0 || result.hasTool()) {
            sender.sendMessage(heldLine("Herramienta", result.tool()));
        }
        sender.sendMessage("§7  Progreso: §f" + decimal(result.progress()) + " §7-> curva §f" + result.curveType()
            + " §7-> §f" + decimal(result.curvedProgress()));
        if (module.service().settings().memory().enabled()) {
            ToffyResult real = module.evaluateReal(target);
            sender.sendMessage("§7  Nivel real: §d" + level(real.value(), decimals)
                + " §8(cuenta lo que tenía antes de sacárselo o cambiarlo por algo peor)");
        }
    }

    private static String heldLine(String label, ToffyResult.Held held) {
        if (!held.present()) {
            return "§7  " + label + ": §fninguna";
        }
        return "§7  " + label + ": §f" + familyName(held.family()) + " " + percent(held.score())
            + " §8(" + held.item().key() + ", poder " + format(held.power()) + ")";
    }

    private void reload(CommandSender sender) {
        ToffyConfigLoadResult result = module.reload();
        if (module.reloadMessages()) {
            sender.sendMessage(PREFIX + "§atoffy_mensajes.yml recargado.");
        } else {
            sender.sendMessage(PREFIX + "§ctoffy_mensajes.yml tiene errores; se mantienen los mensajes anteriores.");
        }
        if (result.valid()) {
            sender.sendMessage(PREFIX + "§atoffy_complementaries.yml y toffy_valores.yml recargados"
                + (result.warnings().isEmpty() ? "." : " con " + result.warnings().size() + " avisos (ver consola)."));
            return;
        }
        sender.sendMessage(PREFIX + "§cLa configuracion tiene " + result.errors().size()
            + " errores; se mantiene la configuracion anterior:");
        result.errors().stream().limit(MAX_ERRORS_SHOWN).forEach(error -> sender.sendMessage("§c  - " + error));
        if (result.errors().size() > MAX_ERRORS_SHOWN) {
            sender.sendMessage("§c  ... y " + (result.errors().size() - MAX_ERRORS_SHOWN) + " mas en la consola.");
        }
    }

    private void curve(CommandSender sender) {
        ToffyService service = module.service();
        ToffyCurve curve = service.settings().curve();
        int decimals = service.settings().decimals();
        sender.sendMessage(PREFIX + "Curva §f" + curve.type() + "§7 (progreso -> nivel):");
        StringBuilder line = new StringBuilder("§7  ");
        for (int step = 0; step <= 10; step++) {
            double progress = step / 10.0;
            line.append("§f").append(decimal(progress)).append("§8->§e")
                .append(level(service.maximum() * curve.apply(progress), decimals));
            if (step < 10) {
                line.append("§8, ");
            }
            if (step == 5) {
                sender.sendMessage(line.toString());
                line = new StringBuilder("§7  ");
            }
        }
        sender.sendMessage(line.toString());
    }

    private static boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission(permission) || sender.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        sender.sendMessage("§cNo tenes permiso para usar este comando.");
        return false;
    }

    static String familyName(String family) {
        return switch (family) {
            case "sword" -> "espada";
            case "axe" -> "hacha";
            case "mace" -> "maza";
            case "spear" -> "lanza";
            case "trident" -> "tridente";
            case "pickaxe" -> "pico";
            case "shovel" -> "pala";
            case "hoe" -> "azada";
            case "shears" -> "tijeras";
            case "fishing_rod" -> "cana de pescar";
            default -> family;
        };
    }

    static String level(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length > 1) {
            if (args[0].equalsIgnoreCase("torneo") || args[0].equalsIgnoreCase("tp")) {
                return tournamentCommand.complete(sender, args);
            }
            return List.of();
        }
        List<String> options = new ArrayList<>();
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            options.add("reload");
            options.add("curva");
        }
        if (sender.hasPermission(TORNEO_ADMIN_PERMISSION) || sender.hasPermission(ADMIN_PERMISSION)) {
            options.add("torneo");
            options.add("tp");
        }
        if (sender.hasPermission(CHECK_OTHERS_PERMISSION) || sender.hasPermission(ADMIN_PERMISSION)) {
            module.server().getOnlinePlayers().forEach(player -> options.add(player.getName()));
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
