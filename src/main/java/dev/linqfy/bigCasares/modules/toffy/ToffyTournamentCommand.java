package dev.linqfy.bigCasares.modules.toffy;

import org.bukkit.Location;
import org.bukkit.World;
import dev.linqfy.bigCasares.modules.toffy.eventos.ToffyEventType;
import dev.linqfy.bigCasares.modules.toffy.eventos.ToffyEvents;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.UncheckedIOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
        List.of("crear", "lista", "ver", "abrir", "cerrar", "lugar", "sacar", "mensaje", "borrar");
    private static final List<String> TP_SUBCOMMANDS =
        List.of("concursantes", "devolver", "programar", "programados");
    private static final List<String> EVENT_SUBCOMMANDS = List.of("iniciar", "detener", "tipos", "estado");
    private static final String EVENT_USAGE =
        "§cUso: /toffy evento <iniciar <tipo> <torneo> [segundos]|detener|tipos|estado>";

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
            case "lugar" -> {
                if (needs(sender, args, 3, "/toffy torneo lugar <torneo> [borrar]")) {
                    place(sender, tournaments, args);
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
            default -> sender.sendMessage("§cUso: /toffy torneo <crear|lista|ver|abrir|cerrar|lugar|sacar|mensaje|borrar>"
                + " (mira /toffy ayuda)");
        }
    }

    /**
     * {@code /toffy evento ...}: staff start, stop and inspect events. An event always runs on a tournament's
     * sign-up list.
     */
    void event(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ToffyEvents.ADMIN_PERMISSION) && !sender.hasPermission(ToffyCommand.ADMIN_PERMISSION)) {
            sender.sendMessage("§cNo tenes permiso para usar este comando.");
            return;
        }
        ToffyEvents events = module.events();
        if (events == null) {
            sender.sendMessage(ToffyEvents.UNAVAILABLE_TEXT);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(EVENT_USAGE);
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "iniciar" -> start(sender, events, args);
            case "detener" -> events.stop(sender);
            case "tipos" -> {
                for (ToffyEventType type : ToffyEventType.values()) {
                    sender.sendMessage("§e" + type.id() + " §7— " + type.displayName()
                        + (type.arena() ? " §8(arena, inventario de evento)" : " §8(mundo normal, keep inventory)"));
                }
            }
            case "estado" -> events.status().forEach(sender::sendMessage);
            default -> sender.sendMessage("§cSubcomando desconocido. Mira /toffy ayuda.");
        }
    }

    private void start(CommandSender sender, ToffyEvents events, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUso: /toffy evento iniciar <tipo> <torneo> [segundos de espera 5-600]");
            return;
        }
        Optional<ToffyEventType> type = ToffyEventType.parse(args[2]);
        if (type.isEmpty()) {
            sender.sendMessage("§cTipo desconocido. Usa /toffy evento tipos.");
            return;
        }
        ToffyTournamentService tournaments = module.tournaments();
        Optional<ToffyTournament> tournament = tournaments == null ? Optional.empty() : tournaments.find(args[3]);
        if (tournament.isEmpty()) {
            sender.sendMessage("§cNo existe el torneo " + args[3] + ".");
            return;
        }
        Integer lobby = null;
        if (args.length >= 5) {
            try {
                lobby = Integer.parseInt(args[4]);
            } catch (NumberFormatException invalid) {
                lobby = -1;
            }
            if (lobby < 5 || lobby > 600) {
                sender.sendMessage("§cLa espera tiene que estar entre 5 y 600 segundos.");
                return;
            }
        }
        events.start(sender, type.get(), tournament.get().key(), lobby);
    }

    /** Tab completion for {@code /toffy evento ...}; {@code args[0]} is "evento". */
    List<String> completeEvent(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ToffyEvents.ADMIN_PERMISSION) && !sender.hasPermission(ToffyCommand.ADMIN_PERMISSION)) {
            return List.of();
        }
        ToffyTournamentService tournaments = module.tournaments();
        List<String> options = new ArrayList<>();
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "";
        if (args.length == 2) {
            options.addAll(EVENT_SUBCOMMANDS);
        } else if (args.length == 3 && sub.equals("iniciar")) {
            for (ToffyEventType type : ToffyEventType.values()) {
                options.add(type.id());
            }
        } else if (args.length == 4 && sub.equals("iniciar") && tournaments != null) {
            options.addAll(tournaments.keys());
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
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
                + " §8- §e" + tournament.participants().size() + "§7 participantes" + scheduleSuffix(tournament));
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
        sender.sendMessage("§7  Lugar: " + (tournament.place() == null
            ? "§8sin lugar (usa /toffy torneo lugar " + tournament.key() + ")"
            : "§f" + placeText(tournament.place())));
        if (tournament.scheduledAt() != null) {
            sender.sendMessage("§7  Teletransporte programado: §b"
                + ToffyTournamentTime.dateTime(tournament.scheduledAt(), zone()));
        }
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
        ToffyEvents events = module.events();
        if (events != null) {
            events.withdraw(target, found.get().key());
        }
        sender.sendMessage(PREFIX + "§a" + playerName + " fue sacado del torneo " + found.get().displayName() + ".");
    }

    /** {@code /toffy torneo lugar <torneo> [borrar]}: the sender's location becomes the place of the tournament. */
    private void place(CommandSender sender, ToffyTournamentService tournaments, String[] args) {
        Optional<ToffyTournament> found = tournaments.find(args[2]);
        if (found.isEmpty()) {
            sender.sendMessage("§cNo existe el torneo " + args[2] + ".");
            return;
        }
        ToffyTournament tournament = found.get();
        if (args.length > 3 && args[3].equalsIgnoreCase("borrar")) {
            if (tournament.place() == null) {
                sender.sendMessage("§eEl torneo " + tournament.displayName() + " no tenia lugar.");
                return;
            }
            boolean scheduled = tournament.scheduledAt() != null;
            tournaments.clearPlace(tournament.key());
            sender.sendMessage(PREFIX + "§aLugar del torneo " + tournament.displayName() + " borrado"
                + (scheduled ? "; el teletransporte programado tambien se cancelo." : "."));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDesde consola no hay lugar: ubicate en el juego y usa /toffy torneo lugar "
                + tournament.key() + ".");
            return;
        }
        ToffyStoredLocation saved = ToffyModule.stored(player.getLocation());
        tournaments.setPlace(tournament.key(), saved);
        sender.sendMessage(PREFIX + "§aLugar del torneo §e" + tournament.displayName() + "§a guardado: §f"
            + placeText(saved) + "§a.");
    }

    /** {@code /toffy tp programar <torneo> <HH:mm> [dd/MM]} and {@code ... cancelar}. */
    private void schedule(CommandSender sender, ToffyTournamentService tournaments, String[] args) {
        if (!needs(sender, args, 4,
            "/toffy tp programar <torneo> <HH:mm> [dd/MM] | /toffy tp programar <torneo> cancelar")) {
            return;
        }
        Optional<ToffyTournament> found = tournaments.find(args[2]);
        if (found.isEmpty()) {
            sender.sendMessage("§cNo existe el torneo " + args[2] + ".");
            return;
        }
        ToffyTournament tournament = found.get();
        ZoneId zone = zone();
        if (args[3].equalsIgnoreCase("cancelar")) {
            Long at = tournament.scheduledAt();
            if (at == null) {
                sender.sendMessage("§eEl torneo " + tournament.displayName()
                    + " no tenia un teletransporte programado.");
                return;
            }
            tournaments.cancelSchedule(tournament.key());
            sender.sendMessage(PREFIX + "§aTeletransporte programado de " + tournament.displayName() + " cancelado.");
            module.send(sender, ToffyMessages.TP_CANCELADO, tournament.key(),
                scheduleTexts(tournament, at, zone));
            return;
        }
        if (tournament.place() == null) {
            sender.sendMessage("§cEl torneo no tiene lugar. Usa /toffy torneo lugar " + tournament.key());
            return;
        }
        long at;
        try {
            at = ToffyTournamentTime.resolve(args[3], args.length > 4 ? args[4] : null, tournaments.now(), zone);
        } catch (IllegalArgumentException invalid) {
            sender.sendMessage("§c" + invalid.getMessage());
            return;
        }
        Optional<Long> replaced = tournaments.schedule(tournament.key(), at);
        replaced.ifPresent(old -> sender.sendMessage(PREFIX + "§eSe reemplazo el teletransporte programado para "
            + ToffyTournamentTime.dateTime(old, zone) + "."));
        sender.sendMessage(PREFIX + "§aTeletransporte de " + tournament.displayName() + " programado para §e"
            + ToffyTournamentTime.dateTime(at, zone) + "§a.");
        String announcement = module.message(ToffyMessages.TP_PROGRAMADO, tournament.key(),
            scheduleTexts(tournament, at, zone));
        if (announcement != null) {
            module.server().broadcastMessage(announcement);
        }
    }

    /** {@code /toffy tp programados}: every scheduled teleport. */
    private void scheduled(CommandSender sender, ToffyTournamentService tournaments) {
        List<ToffyTournament> all = tournaments.list().stream()
            .filter(tournament -> tournament.scheduledAt() != null)
            .sorted(Comparator.comparingLong(tournament -> tournament.scheduledAt()))
            .toList();
        if (all.isEmpty()) {
            sender.sendMessage(PREFIX + "No hay teletransportes programados. Usa /toffy tp programar <torneo> <HH:mm>.");
            return;
        }
        sender.sendMessage(PREFIX + "Teletransportes programados (" + all.size() + "):");
        for (ToffyTournament tournament : all) {
            sender.sendMessage("§7  §f" + tournament.displayName() + " §8- §b"
                + ToffyTournamentTime.dateTime(tournament.scheduledAt(), zone()) + " §8- §7lugar: §f"
                + (tournament.place() == null ? "§csin lugar" : placeText(tournament.place())));
        }
    }

    static String scheduleSuffix(ToffyTournament tournament) {
        return tournament.scheduledAt() == null ? ""
            : " §8- §bprogramado " + ToffyTournamentTime.dateTime(tournament.scheduledAt(), zone());
    }

    private static Map<String, String> scheduleTexts(ToffyTournament tournament, long at, ZoneId zone) {
        Map<String, String> map = new HashMap<>();
        map.put("torneo", tournament.displayName());
        map.put("hora", ToffyTournamentTime.hour(at, zone));
        map.put("fecha", ToffyTournamentTime.date(at, zone));
        return map;
    }

    private static String placeText(ToffyStoredLocation place) {
        return place.world() + " " + ToffyModule.coordinate(place.x()) + ", " + ToffyModule.coordinate(place.y())
            + ", " + ToffyModule.coordinate(place.z());
    }

    private static ZoneId zone() {
        return ZoneId.systemDefault();
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
        switch (sub) {
            case "concursantes" -> {
                if (needs(sender, args, 3, "/toffy tp concursantes <torneo> [lugar | x y z [mundo]]")) {
                    bring(sender, tournaments, args);
                }
            }
            case "devolver" -> {
                if (needs(sender, args, 3, "/toffy tp devolver <torneo>")) {
                    giveBack(sender, tournaments, args[2]);
                }
            }
            case "programar" -> schedule(sender, tournaments, args);
            case "programados" -> scheduled(sender, tournaments);
            default -> sender.sendMessage("§cUso: /toffy tp <concursantes|devolver|programar|programados> (mira /toffy ayuda)");
        }
    }

    private void bring(CommandSender sender, ToffyTournamentService tournaments, String[] args) {
        Optional<ToffyTournament> found = tournaments.find(args[2]);
        if (found.isEmpty()) {
            sender.sendMessage("§cNo existe el torneo " + args[2] + ".");
            return;
        }
        ToffyTournament tournament = found.get();
        Location target = args.length > 3 && args[3].equalsIgnoreCase("lugar")
            ? savedPlace(sender, tournament)
            : target(sender, args);
        if (target == null) {
            return;
        }
        ToffyModule.Brought result = module.bringParticipants(tournaments, tournament, target);
        sender.sendMessage(PREFIX + "§a" + result.moved() + " concursantes llevados§7, §e" + result.offline()
            + " offline" + (result.failed() > 0 ? "§7, §c" + result.failed() + " no se pudieron mover" : "") + "§7.");
    }

    private Location savedPlace(CommandSender sender, ToffyTournament tournament) {
        ToffyStoredLocation place = tournament.place();
        if (place == null) {
            sender.sendMessage("§cEl torneo no tiene lugar. Usa /toffy torneo lugar " + tournament.key());
            return null;
        }
        Location target = module.savedLocation(place);
        if (target == null) {
            sender.sendMessage("§cEl mundo " + place.world() + " del lugar del torneo no esta cargado.");
        }
        return target;
    }

    private Location target(CommandSender sender, String[] args) {
        if (args.length == 3) {
            if (sender instanceof Player player) {
                return player.getLocation();
            }
            sender.sendMessage("§cDesde consola usa /toffy tp concursantes <torneo> lugar o <torneo> <x> <y> <z> [mundo].");
            return null;
        }
        if (args.length < 6) {
            sender.sendMessage("§cUso: /toffy tp concursantes <torneo> [lugar | x y z [mundo]]");
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
        } else if (tournaments != null && args.length == 3 && !sub.equals("crear") && !sub.equals("lista")
            && !sub.equals("programados")) {
            options.addAll(tournaments.keys());
        } else if (tp && args.length == 4 && sub.equals("concursantes")) {
            options.add("lugar");
        } else if (tp && args.length == 4 && sub.equals("programar")) {
            options.add("cancelar");
            options.add(nextFiveMinutes(zone()));
        } else if (!tp && args.length == 4 && sub.equals("lugar")) {
            options.add("borrar");
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

    /** The first HH:mm that is a multiple of five minutes after now, to suggest for {@code tp programar}. */
    private static String nextFiveMinutes(ZoneId zone) {
        ZonedDateTime next = ZonedDateTime.now(zone).truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        int remainder = next.getMinute() % 5;
        if (remainder != 0) {
            next = next.plusMinutes(5 - remainder);
        }
        return ToffyTournamentTime.hour(next.toInstant().toEpochMilli(), zone);
    }
}
