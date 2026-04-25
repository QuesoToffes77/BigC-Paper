package dev.linqfy.bigCasares.command;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.modules.copperapple.CopperAppleCommand;
import dev.linqfy.bigCasares.modules.skillrating.SkillRatingView;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class BigCasaresCommand implements CommandExecutor, TabCompleter {

    private static final String SHOP_OPEN_PERMISSION = "bigcasares.shop.open";
    private static final String SHOP_RELOAD_PERMISSION = "bigcasares.shop.reload";
    private static final String RATING_VIEW_PERMISSION = "bigcasares.skillrating.view";
    private static final String RATING_VIEW_OTHERS_PERMISSION = "bigcasares.skillrating.view.others";

    private final BigCasares plugin;
    private final CopperAppleCommand legacyCommand;

    public BigCasaresCommand(BigCasares plugin) {
        this.plugin = plugin;
        this.legacyCommand = new CopperAppleCommand(plugin);
    }

    public static boolean isReload(String value) {
        return "reload".equalsIgnoreCase(value);
    }

    public static boolean isShop(String value) {
        return "shop".equalsIgnoreCase(value);
    }

    public static boolean isRating(String value) {
        return "rating".equalsIgnoreCase(value);
    }

    public static List<String> rootSuggestions(String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        return List.of("give", "misiones", "shop", "rating", "reload").stream()
            .filter(option -> option.startsWith(lowered))
            .collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("shop".equalsIgnoreCase(command.getName())) {
            return openShop(sender);
        }
        if ("rating".equalsIgnoreCase(command.getName())) {
            return showRating(sender, args);
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        if (isReload(args[0])) {
            if (!sender.hasPermission(SHOP_RELOAD_PERMISSION)) {
                sender.sendMessage(ChatColor.RED + "No tenes permiso para recargar configs.");
                return true;
            }
            plugin.reloadPluginState();
            sender.sendMessage(ChatColor.GREEN + "Configs recargadas.");
            return true;
        }

        if (isShop(args[0])) {
            return openShop(sender);
        }

        if (isRating(args[0])) {
            return showRating(sender, dropFirst(args));
        }

        return legacyCommand.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("shop".equalsIgnoreCase(command.getName())) {
            return List.of();
        }
        if ("rating".equalsIgnoreCase(command.getName())) {
            return List.of();
        }
        if (args.length == 1) {
            return rootSuggestions(args[0]);
        }
        return legacyCommand.onTabComplete(sender, command, alias, args);
    }

    private boolean openShop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores pueden abrir el shop.");
            return true;
        }
        if (!sender.hasPermission(SHOP_OPEN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "No tenes permiso para abrir el shop.");
            return true;
        }
        plugin.openShop(player);
        return true;
    }

    private boolean showRating(CommandSender sender, String[] args) {
        if (!sender.hasPermission(RATING_VIEW_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "No tenes permiso para ver skill rating.");
            return true;
        }
        if (plugin.getSkillRatingModule() == null || !plugin.getSkillRatingModule().isEnabled()) {
            sender.sendMessage(ChatColor.RED + "Skill rating no esta disponible.");
            return true;
        }

        OfflinePlayer target;
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Uso: /rating <player>");
                return true;
            }
            target = player;
        } else {
            if (!sender.hasPermission(RATING_VIEW_OTHERS_PERMISSION)) {
                sender.sendMessage(ChatColor.RED + "No tenes permiso para ver ratings de otros jugadores.");
                return true;
            }
            target = plugin.getServer().getOfflinePlayer(args[0]);
        }

        var state = plugin.getSkillRatingModule().service().ratingFor(target.getUniqueId());
        sender.sendMessage(ChatColor.AQUA + SkillRatingView.format(target.getName() == null ? target.getUniqueId().toString() : target.getName(), state));
        return true;
    }

    private String[] dropFirst(String[] args) {
        if (args.length <= 1) {
            return new String[0];
        }
        String[] result = new String[args.length - 1];
        System.arraycopy(args, 1, result, 0, result.length);
        return result;
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " give <item_id> [player] [amount]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " misiones [diarias|semanales|reclamar]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " shop");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " rating [player]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " reload");
    }
}
