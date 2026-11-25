package dev.linqfy.bigCasares.modules.toffy;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * {@code /anotar <torneo>}: a player signs up for a tournament.
 */
final class ToffyAnotarCommand implements CommandExecutor, TabCompleter {
    static final String PERMISSION = "bigcasares.toffy.anotar";

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
        if (args.length != 1) {
            sender.sendMessage("§cUso: /" + label + " <torneo>");
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
            }
            case ALREADY_REGISTERED -> module.send(sender, ToffyMessages.YA_ANOTADO, key, placeholders);
            case CLOSED -> module.send(sender, ToffyMessages.INSCRIPCION_CERRADA, key, placeholders);
            case UNKNOWN_TOURNAMENT -> module.send(sender, ToffyMessages.TORNEO_NO_EXISTE, null, placeholders);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ToffyTournamentService tournaments = module.tournaments();
        if (args.length != 1 || tournaments == null) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return tournaments.list().stream()
            .filter(ToffyTournament::open)
            .map(ToffyTournament::key)
            .filter(key -> key.startsWith(prefix))
            .toList();
    }
}
