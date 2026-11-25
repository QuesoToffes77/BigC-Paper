package dev.linqfy.bigCasares.modules.toffy;

import dev.linqfy.bigCasares.modules.toffy.eventos.ToffyEvents;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code /anotar <torneo>} signs up for a tournament, {@code /anotar salir <torneo>} signs out (and leaves the running
 * staff event of that tournament), {@code /anotar estado} shows the running event and the tournaments of the player.
 */
final class ToffyAnotarCommand implements CommandExecutor, TabCompleter {
    static final String PERMISSION = "bigcasares.toffy.anotar";
    private static final String USAGE = " <torneo> | salir <torneo> | estado";

    private final ToffyModule module;

    ToffyAnotarCommand(ToffyModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden anotarse.");
            return true;
        }
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage("§cNo tenes permiso para usar este comando.");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("estado")) {
            status(player);
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("salir")) {
            if (args.length != 2) {
                sender.sendMessage("§cUso: /" + label + " salir <torneo>");
                return true;
            }
            signOut(player, args[1]);
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§cUso: /" + label + USAGE);
            return true;
        }
        ToffyTournamentService tournaments = module.tournaments();
        if (tournaments == null) {
            sender.sendMessage("§cToffy Complementaries no esta disponible.");
            return true;
        }
        ToffyTournamentService.RegisterResult result;
        try {
            result = tournaments.register(args[0], player.getUniqueId(), player.getName());
        } catch (UncheckedIOException failure) {
            sender.sendMessage("§cNo se pudo guardar tu inscripcion: " + failure.getCause().getMessage());
            return true;
        }
        Optional<ToffyTournament> tournament = tournaments.find(args[0]);
        String key = tournament.map(ToffyTournament::key).orElse(null);
        Map<String, String> placeholders = Map.of(
            "jugador", player.getName(),
            "torneo", tournament.map(ToffyTournament::displayName).orElse(args[0]),
            "cantidad", String.valueOf(tournament.map(found -> found.participants().size()).orElse(0)));
        switch (result) {
            case REGISTERED -> {
                module.send(sender, ToffyMessages.ANOTADO, key, placeholders);
                String announcement = module.message(ToffyMessages.ANUNCIO_ANOTADO, key, placeholders);
                if (announcement != null) {
                    module.server().broadcastMessage(announcement);
                }
                joinLobby(player, key);
            }
            case ALREADY_REGISTERED -> {
                module.send(sender, ToffyMessages.YA_ANOTADO, key, placeholders);
                joinLobby(player, key);
            }
            case CLOSED -> module.send(sender, ToffyMessages.INSCRIPCION_CERRADA, key, placeholders);
            case UNKNOWN_TOURNAMENT -> module.send(sender, ToffyMessages.TORNEO_NO_EXISTE, null, placeholders);
        }
        return true;
    }

    /** Someone who signs up (or was already signed up) while the event lobby of the tournament is open joins it. */
    private void joinLobby(Player player, String key) {
        ToffyEvents events = module.events();
        if (events != null && key != null) {
            events.joinLobby(player, key);
        }
    }

    private void signOut(Player player, String name) {
        ToffyTournamentService tournaments = module.tournaments();
        if (tournaments == null) {
            player.sendMessage("§cToffy Complementaries no esta disponible.");
            return;
        }
        Optional<ToffyTournament> found = tournaments.find(name);
        if (found.isEmpty()) {
            player.sendMessage("§cNo existe el torneo " + name + ".");
            return;
        }
        ToffyTournament tournament = found.get();
        if (!tournament.participants().containsKey(player.getUniqueId())) {
            player.sendMessage("§cNo estas anotado en el torneo " + tournament.displayName() + ".");
            return;
        }
        ToffyEvents events = module.events();
        if (events != null) {
            events.withdraw(player.getUniqueId(), tournament.key());
        }
        try {
            tournaments.removeParticipant(tournament.key(), player.getUniqueId());
        } catch (UncheckedIOException failure) {
            player.sendMessage("§cNo se pudo guardar tu cambio: " + failure.getCause().getMessage());
            return;
        }
        player.sendMessage("§aSaliste del torneo §e" + tournament.displayName() + "§a.");
        if (events == null) {
            player.sendMessage(ToffyEvents.UNAVAILABLE_TEXT);
        }
    }

    private void status(Player player) {
        ToffyEvents events = module.events();
        if (events == null) {
            player.sendMessage(ToffyEvents.UNAVAILABLE_TEXT);
        } else {
            events.status().forEach(player::sendMessage);
            if (events.participates(player.getUniqueId())) {
                player.sendMessage("§aEstas dentro del evento. Para salir: /anotar salir <torneo>.");
            }
        }
        ToffyTournamentService tournaments = module.tournaments();
        List<String> signedUp = tournaments == null ? List.of() : tournaments.list().stream()
            .filter(tournament -> tournament.participants().containsKey(player.getUniqueId()))
            .map(ToffyTournament::displayName)
            .toList();
        player.sendMessage(signedUp.isEmpty()
            ? "§7No estas anotado en ningun torneo."
            : "§7Anotado en: §f" + String.join("§7, §f", signedUp));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ToffyTournamentService tournaments = module.tournaments();
        if (tournaments == null || args.length == 0 || args.length > 2) {
            return List.of();
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> options;
        if (args.length == 1) {
            options = tournaments.list().stream()
                .filter(ToffyTournament::open)
                .map(ToffyTournament::key)
                .collect(Collectors.toCollection(ArrayList::new));
            options.add("salir");
            options.add("estado");
        } else if (args[0].equalsIgnoreCase("salir") && sender instanceof Player player) {
            options = tournaments.list().stream()
                .filter(tournament -> tournament.participants().containsKey(player.getUniqueId()))
                .map(ToffyTournament::key)
                .toList();
        } else {
            return List.of();
        }
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
