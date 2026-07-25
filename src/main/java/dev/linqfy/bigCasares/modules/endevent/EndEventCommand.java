package dev.linqfy.bigCasares.modules.endevent;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EndEventCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "bigcasares.endevent.admin";

    private final EndEventRuntime runtime;

    public EndEventCommand(EndEventRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cNo tenés permiso.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            sender.sendMessage(runtime.status());
            return true;
        }
        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "validate" -> runtime.validate().forEach(sender::sendMessage);
            case "optin" -> optIn(sender);
            case "portal" -> portal(sender, args);
            case "arm" -> arm(sender, args);
            case "abort" -> {
                runtime.abort();
                sender.sendMessage("§eEvento abortado y estado previo restaurado.");
            }
            case "reset" -> {
                runtime.reset();
                sender.sendMessage("§aEstado del evento reiniciado.");
            }
            case "debug" -> debug(sender, args);
            default -> usage(sender);
        }
        return true;
    }

    private void optIn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo un operador dentro del juego puede inscribirse.");
            return;
        }
        if (runtime.optIn(player)) {
            sender.sendMessage("§aVas a contar y competir en el evento.");
        }
    }

    private void portal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /endevent portal <set|clear>");
            return;
        }
        if (args[1].equalsIgnoreCase("clear")) {
            runtime.clearPortal();
            sender.sendMessage("§eFallback del portal eliminado.");
            return;
        }
        if (args[1].equalsIgnoreCase("set") && sender instanceof Player player) {
            runtime.setPortal(player.getLocation());
            sender.sendMessage("§aFallback del portal guardado en tu posición.");
            return;
        }
        sender.sendMessage("§c`portal set` requiere un jugador dentro del juego.");
    }

    private void arm(CommandSender sender, String[] args) {
        try {
            LocalDate date = args.length >= 2 ? LocalDate.parse(args[1]) : LocalDate.now();
            runtime.arm(date);
            sender.sendMessage("§aEvento armado para " + date + ".");
        } catch (DateTimeParseException failure) {
            sender.sendMessage("§cFecha inválida. Usá YYYY-MM-DD.");
        }
    }

    private void debug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /endevent debug <countdown|reveal|dragon-dead|hunter|elapsed|winner>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        try {
            switch (action) {
                case "countdown" -> {
                    int seconds = args.length >= 3 ? Integer.parseInt(args[2]) : 10;
                    runtime.debugCountdown(seconds);
                    sender.sendMessage("§aCountdown de prueba iniciado.");
                }
                case "reveal" -> sender.sendMessage(runtime.debugReveal()
                    ? "§aFase de dragón activada."
                    : "§cNo hay portal resuelto; usá /endevent portal set.");
                case "dragon-dead" -> {
                    runtime.debugDragonDead();
                    sender.sendMessage("§aFase de huevo activada.");
                }
                case "hunter" -> {
                    Player target = requirePlayer(sender, args);
                    if (target != null) {
                        runtime.debugHunter(target);
                        sender.sendMessage("§aCacería iniciada con " + target.getName() + ".");
                    }
                }
                case "elapsed" -> {
                    int minutes = args.length >= 3 ? Integer.parseInt(args[2]) : 0;
                    runtime.debugElapsed(minutes);
                    sender.sendMessage("§aTiempo de cacería ajustado a " + minutes + " minutos.");
                }
                case "winner" -> {
                    Player target = requirePlayer(sender, args);
                    if (target != null) {
                        runtime.previewWinner(target);
                        sender.sendMessage("§eCelebración mostrada sin aplicar bans.");
                    }
                }
                default -> sender.sendMessage("§cSubcomando debug desconocido.");
            }
        } catch (NumberFormatException failure) {
            sender.sendMessage("§cEl tiempo debe ser un número entero.");
        } catch (IllegalStateException | IllegalArgumentException failure) {
            sender.sendMessage("§c" + failure.getMessage());
        }
    }

    private Player requirePlayer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cFalta el nombre del jugador.");
            return null;
        }
        Player player = org.bukkit.Bukkit.getPlayerExact(args[2]);
        if (player == null) {
            sender.sendMessage("§cEse jugador no está conectado.");
        }
        return player;
    }

    private void usage(CommandSender sender) {
        sender.sendMessage("§e/endevent <status|validate|optin|portal|arm|abort|reset|debug>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> candidates = new ArrayList<>();
        if (args.length == 1) {
            candidates.addAll(List.of("status", "validate", "optin", "portal", "arm", "abort", "reset", "debug"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("portal")) {
            candidates.addAll(List.of("set", "clear"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            candidates.addAll(List.of("countdown", "reveal", "dragon-dead", "hunter", "elapsed", "winner"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("debug")
            && (args[1].equalsIgnoreCase("hunter") || args[1].equalsIgnoreCase("winner"))) {
            org.bukkit.Bukkit.getOnlinePlayers().forEach(player -> candidates.add(player.getName()));
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return candidates.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(prefix)).sorted().toList();
    }
}
