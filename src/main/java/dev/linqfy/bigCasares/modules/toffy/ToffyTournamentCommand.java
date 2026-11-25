package dev.linqfy.bigCasares.modules.toffy;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Admin subcommands of {@code /toffy}: {@code torneo ...} and {@code tp ...}.
 */
final class ToffyTournamentCommand {
    static final String PERMISSION = "bigcasares.toffy.torneo.admin";
    private static final String PREFIX = "§6Toffy §8» §7";
    private static final List<String> TORNEO_SUBCOMMANDS =
        List.of("crear", "cerrar", "abrir", "borrar", "lista", "ver", "sacar", "mensaje");
    private static final List<String> TP_SUBCOMMANDS = List.of("concursantes", "devolver");

    private final ToffyModule module;

    ToffyTournamentCommand(ToffyModule module) {
        this.module = module;
    }

    void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION) && !sender.hasPermission(ToffyCommand.ADMIN_PERMISSION)) {
            sender.sendMessage("§cNo tenes permiso para usar este comando.");
            return;
        }
        ToffyTournamentService tournaments = module.tournaments();
        if (tournaments == null) {
            sender.sendMessage("§cToffy Complementaries no esta disponible.");
            return;
        }
        try {
            if (args[0].equalsIgnoreCase("tp")) {
                teleport(sender, tournaments, args);
            } else {
                tournament(sender, tournaments, args);
            }
        } catch (UncheckedIOException failure) {
            sender.sendMessage("§cNo se pudo guardar torneos.yml: " + failure.getCause().getMessage());
        }
    }

    private void tournament(CommandSender sender, ToffyTournamentService tournaments, String[] args) {
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "crear" -> create(sender, tournaments, args);
            case "cerrar" -> {
                if (needs(sender, args, 3, "/toffy torneo cerrar <nombre> [mensaje...]")) {
                    close(sender, tournaments, args);
                }
            }
            case "abrir" -> {
                if (needs(sender, args, 3, "/toffy torneo abrir <nombre>")) {
                    open(sender, tournaments, args[2]);
                }
            }
            case "borrar" -> {
                if (needs(sender, args, 3, "/toffy torneo borrar <nombre> [forzar]")) {
                    int saved = tournaments.savedPlaces(args[2]);
                    boolean force = args.length > 3 && args[3].equalsIgnoreCase("forzar");
                    if (saved > 0 && !force) {
                        sender.sendMessage("§c" + saved + " jugador(es) tienen un lugar guardado en " + args[2]
                            + ". Usa /toffy tp devolver " + args[2] + " primero, o /toffy torneo borrar "
                            + args[2] + " forzar para perder esos lugares.");
                        return;
                    }
                    sender.sendMessage(tournaments.delete(args[2])
                        ? PREFIX + "§aTorneo " + args[2] + " borrado."
                        : "§cNo existe el torneo " + args[2] + ".");
                }
            }
            case "lista" -> list(sender, tournaments);
            case "ver" -> {
                if (needs(sender, args, 3, "/toffy torneo ver <nombre>")) {
                    view(sender, tournaments, args[2]);
                }
            }
            case "sacar" -> {
                if (needs(sender, args, 4, "/toffy torneo sacar <nombre> <jugador>")) {
                    remove(sender, tournaments, args[2], args[3]);
                }
            }
            case "mensaje" -> {
                if (needs(sender, args, 5, "/toffy torneo mensaje <nombre> <caso> <texto...|-|reset>")) {
                    message(sender, tournaments, args);
                }
            }
            default -> sender.sendMessage("§cUso: /toffy torneo <crear|cerrar|abrir|borrar|lista|ver|sacar|mensaje>");
        }
    }

    private static boolean needs(CommandSender sender, String[] args, int length, String usage) {
        if (args.length >= length) {
            return true;
        }
        sender.sendMessage("§cUso: " + usage);
        return false;
    }

    private void create(CommandSender sender, ToffyTournamentService tournaments, String[] args) {
        if (!needs(sender, args, 3, "/toffy torneo crear <nombre>")) {
            return;
        }
        try {
            ToffyTournament created = tournaments.create(args[2]);
            sender.sendMessage(PREFIX + "§aTorneo §e" + created.displayName()
                + "§a creado. Los jugadores se anotan con §e/anotar " + created.key() + "§a.");
        } catch (IllegalArgumentException | IllegalStateException invalid) {
            sender.sendMessage("§c" + invalid.getMessage());
        }
    }

    private void close(CommandSender sender, ToffyTournamentService tournaments, String[] args) {
        ToffyTournamentService.Change change = tournaments.close(args[2]);
        if (change == ToffyTournamentService.Change.UNKNOWN) {
            sender.sendMessage("§cNo existe el torneo " + args[2] + ".");
            return;
        }
        if (change == ToffyTournamentService.Change.UNCHANGED) {
            sender.sendMessage(PREFIX + "§eLas inscripciones ya estaban cerradas.");
            return;
        }
        ToffyTournament tournament = tournaments.find(args[2]).orElseThrow();
        Map<String, String> placeholders = placeholders(tournament, null);
        String announcement = args.length > 3
            ? ToffyMessages.format(String.join(" ", Arrays.copyOfRange(args, 3, args.length)), placeholders)
            : module.message(ToffyMessages.CIERRE, tournament.key(), placeholders);
        sender.sendMessage(PREFIX + "§aInscripciones del torneo §e" + tournament.displayName() + "§a cerradas.");
        if (announcement != null) {
            module.server().broadcastMessage(announcement);
        }
    }

    private void open(CommandSender sender, ToffyTournamentService tournaments, String name) {
        ToffyTournamentService.Change change = tournaments.open(name);
        if (change == ToffyTournamentService.Change.UNKNOWN) {
            sender.sendMessage("§cNo existe el torneo " + name + ".");
            return;
        }
        if (change == ToffyTournamentService.Change.UNCHANGED) {
            sender.sendMessage(PREFIX + "§eLas inscripciones ya estaban abiertas.");
            return;
        }
        ToffyTournament tournament = tournaments.find(name).orElseThrow();
        sender.sendMessage(PREFIX + "§aInscripciones del torneo §e" + tournament.displayName() + "§a abiertas.");
        String announcement = module.message(ToffyMessages.APERTURA, tournament.key(), placeholders(tournament, null));
        if (announcement != null) {
            module.server().broadcastMessage(announcement);
        }
    }

    private void list(CommandSender sender, ToffyTournamentService tournaments) {
        List<ToffyTournament> all = tournaments.list();
        if (all.isEmpty()) {
            sender.sendMessage(PREFIX + "No hay torneos. Crea uno con /toffy torneo crear <nombre>.");
            return;
        }
        sender.sendMessage(PREFIX + "Torneos (" + all.size() + "):");
        for (ToffyTournament tournament : all) {
            sender.sendMessage("§7  §f" + tournament.displayName() + " §8- "
                + (tournament.open() ? "§aabierto" : "§cinscripcion cerrada")
                + " §8- §e" + tournament.participants().size() + "§7 participantes");
        }
    }

    private void view(CommandSender sender, ToffyTournamentService tournaments, String name) {
        Optional<ToffyTournament> found = tournaments.find(name);
        if (found.isEmpty()) {
            sender.sendMessage("§cNo existe el torneo " + name + ".");
            return;
        }
        ToffyTournament tournament = found.get();
        sender.sendMessage(PREFIX + "§f" + tournament.displayName() + "§7 - "
            + (tournament.open() ? "§aabierto" : "§cinscripcion cerrada") + "§7 - §e"
            + tournament.participants().size() + "§7 participantes:");
        int position = 1;
        for (Map.Entry<UUID, String> entry : tournament.participants().entrySet()) {
            Player online = module.server().getPlayer(entry.getKey());
            sender.sendMessage("§7  " + position++ + ". §f" + (online != null ? online.getName() : entry.getValue())
                + (online != null ? " §a(online)" : " §8(offline)")
                + (tournament.returns().containsKey(entry.getKey()) ? " §6[lugar guardado]" : ""));
        }
        if (!tournament.messages().isEmpty()) {
            sender.sendMessage("§7  Mensajes propios: §f" + String.join(", ", tournament.messages().keySet()));
        }
    }

    private void remove(CommandSender sender, ToffyTournamentService tournaments, String name, String playerName) {
        Optional<ToffyTournament> found = tournaments.find(name);
        if (found.isEmpty()) {
            sender.sendMessage("§cNo existe el torneo " + name + ".");
            return;
        }
        UUID target = null;
        for (Map.Entry<UUID, String> entry : found.get().participants().entrySet()) {
            if (entry.getValue().equalsIgnoreCase(playerName)) {
                target = entry.getKey();
            }
        }
        if (target == null) {
            sender.sendMessage("§c" + playerName + " no esta anotado en " + found.get().displayName() + ".");
            return;
        }
        tournaments.removeParticipant(name, target);
        sender.sendMessage(PREFIX + "§a" + playerName + " fue sacado del torneo " + found.get().displayName() + ".");
    }

    private void message(CommandSender sender, ToffyTournamentService tournaments, String[] args) {
        Optional<ToffyTournament> found = tournaments.find(args[2]);
        if (found.isEmpty()) {
            sender.sendMessage("§cNo existe el torneo " + args[2] + ".");
            return;
        }
        String caseKey = args[3].toLowerCase(Locale.ROOT);
        if (!ToffyMessages.isCase(caseKey)) {
            sender.sendMessage("§cCaso desconocido. Casos: " + String.join(", ", ToffyMessages.CASES));
            return;
        }
        String key = found.get().key();
        if (args.length == 5 && args[4].equalsIgnoreCase("reset")) {
            sender.sendMessage(tournaments.resetMessage(key, caseKey)
                ? PREFIX + "§aMensaje " + caseKey + " vuelve al de toffy_mensajes.yml."
                : "§eEl torneo no tenia un mensaje propio para " + caseKey + ".");
            return;
        }
        String text = args.length == 5 && args[4].equals("-")
            ? ""
            : String.join(" ", Arrays.copyOfRange(args, 4, args.length));
        tournaments.setMessage(key, caseKey, text);
        sender.sendMessage(PREFIX + "§aMensaje " + caseKey + " del torneo " + found.get().displayName() + ": §r"
            + ToffyMessages.colors(text));
    }

    private void teleport(CommandSender sender, ToffyTournamentService tournaments, String[] args) {
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (sub.equals("concursantes")) {
            if (needs(sender, args, 3, "/toffy tp concursantes <torneo> [x y z [mundo]]")) {
                bring(sender, tournaments, args);
            }
        } else if (sub.equals("devolver")) {
            if (needs(sender, args, 3, "/toffy tp devolver <torneo>")) {
                giveBack(sender, tournaments, args[2]);
            }
        } else {
            sender.sendMessage("§cUso: /toffy tp <concursantes|devolver> <torneo>");
        }
    }

    private void bring(CommandSender sender, ToffyTournamentService tournaments, String[] args) {
        Optional<ToffyTournament> found = tournaments.find(args[2]);
        if (found.isEmpty()) {
            sender.sendMessage("§cNo existe el torneo " + args[2] + ".");
            return;
        }
        Location target = target(sender, args);
        if (target == null) {
            return;
        }
        ToffyTournament tournament = found.get();
        int moved = 0;
        int offline = 0;
        int failed = 0;
        for (UUID id : List.copyOf(tournament.participants().keySet())) {
            Player player = module.server().getPlayer(id);
            if (player == null) {
                offline++;
                continue;
            }
            boolean recorded = tournaments.recordDeparture(tournament.key(), id, ToffyModule.stored(player.getLocation()));
            if (!module.teleportTo(player, target)) {
                if (recorded) {
                    tournaments.clearReturn(tournament.key(), id);
                }
                failed++;
                continue;
            }
            moved++;
            module.send(player, ToffyMessages.LLEVADO, tournament.key(), placeholders(tournament, player, target));
        }
        sender.sendMessage(PREFIX + "§a" + moved + " concursantes llevados§7, §e" + offline + " offline"
            + (failed > 0 ? "§7, §c" + failed + " no se pudieron mover" : "") + "§7.");
    }

    private Location target(CommandSender sender, String[] args) {
        if (args.length == 3) {
            if (sender instanceof Player player) {
                return player.getLocation();
            }
            sender.sendMessage("§cDesde consola usa /toffy tp concursantes <torneo> <x> <y> <z> [mundo].");
            return null;
        }
        if (args.length < 6) {
            sender.sendMessage("§cUso: /toffy tp concursantes <torneo> [x y z [mundo]]");
            return null;
        }
        double x;
        double y;
        double z;
        try {
            x = Double.parseDouble(args[3]);
            y = Double.parseDouble(args[4]);
            z = Double.parseDouble(args[5]);
        } catch (NumberFormatException invalid) {
            sender.sendMessage("§cCoordenadas invalidas.");
            return null;
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            sender.sendMessage("§cCoordenadas invalidas.");
            return null;
        }
        World world;
        if (args.length > 6) {
            world = module.server().getWorld(args[6]);
        } else if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            List<World> worlds = module.server().getWorlds();
            world = worlds.isEmpty() ? null : worlds.getFirst();
        }
        if (world == null) {
            sender.sendMessage("§cMundo no encontrado.");
            return null;
        }
        return new Location(world, x, y, z);
    }

    private void giveBack(CommandSender sender, ToffyTournamentService tournaments, String name) {
        Optional<ToffyTournament> found = tournaments.find(name);
        if (found.isEmpty()) {
            sender.sendMessage("§cNo existe el torneo " + name + ".");
            return;
        }
        ToffyTournament tournament = found.get();
        Set<UUID> ids = new LinkedHashSet<>(tournament.participants().keySet());
        ids.addAll(tournament.returns().keySet());
        int returned = 0;
        int pending = 0;
        int missingWorld = 0;
        int noPlace = 0;
        for (UUID id : ids) {
            Player player = module.server().getPlayer(id);
            ToffyStoredLocation saved = tournament.returns().get(id);
            if (saved == null) {
                if (player != null) {
                    module.send(player, ToffyMessages.SIN_LUGAR_GUARDADO, tournament.key(),
                        placeholders(tournament, player));
                    noPlace++;
                }
            } else if (player == null) {
                if (tournaments.markPending(tournament.key(), id)) {
                    pending++;
                }
            } else if (module.server().getWorld(saved.world()) == null) {
                missingWorld++;
            } else if (module.returnPlayer(player, tournament.key(), saved)) {
                returned++;
            }
        }
        StringBuilder report = new StringBuilder(PREFIX + "§a" + returned + " devueltos§7, §e" + pending
            + " offline (volveran al entrar)");
        if (noPlace > 0) {
            report.append("§7, §e").append(noPlace).append(" sin lugar guardado");
        }
        if (missingWorld > 0) {
            report.append("§7, §c").append(missingWorld)
                .append(" con un mundo que ya no existe (su lugar queda guardado)");
        }
        sender.sendMessage(report.append("§7.").toString());
    }

    private static Map<String, String> placeholders(ToffyTournament tournament, Player player) {
        return placeholders(tournament, player, null);
    }

    static Map<String, String> placeholders(ToffyTournament tournament, Player player, Location at) {
        Map<String, String> map = new HashMap<>();
        map.put("torneo", tournament.displayName());
        map.put("cantidad", String.valueOf(tournament.participants().size()));
        map.put("jugador", player == null ? "" : player.getName());
        if (at != null) {
            map.put("x", ToffyModule.coordinate(at.getX()));
            map.put("y", ToffyModule.coordinate(at.getY()));
            map.put("z", ToffyModule.coordinate(at.getZ()));
            map.put("mundo", at.getWorld() == null ? "" : at.getWorld().getName());
        }
        return map;
    }

    List<String> complete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION) && !sender.hasPermission(ToffyCommand.ADMIN_PERMISSION)) {
            return List.of();
        }
        ToffyTournamentService tournaments = module.tournaments();
        List<String> options = new ArrayList<>();
        String current = args[args.length - 1];
        boolean tp = args[0].equalsIgnoreCase("tp");
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (args.length == 2) {
            options.addAll(tp ? TP_SUBCOMMANDS : TORNEO_SUBCOMMANDS);
        } else if (tournaments != null && args.length == 3 && !sub.equals("crear") && !sub.equals("lista")) {
            options.addAll(tournaments.keys());
        } else if (tournaments != null && args.length == 4 && !tp && sub.equals("sacar")) {
            tournaments.find(args[2]).ifPresent(tournament -> options.addAll(tournament.participants().values()));
        } else if (args.length == 4 && !tp && sub.equals("mensaje")) {
            options.addAll(ToffyMessages.CASES);
        } else if (args.length == 5 && !tp && sub.equals("mensaje")) {
            options.add("reset");
            options.add("-");
        } else if (args.length == 7 && tp && sub.equals("concursantes")) {
            module.server().getWorlds().forEach(world -> options.add(world.getName()));
        }
        String prefix = current.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
