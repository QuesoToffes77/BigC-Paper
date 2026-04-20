package dev.linqfy.bigCasares.command;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.modules.copperapple.CopperAppleCommand;
import org.bukkit.ChatColor;
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

    public static List<String> rootSuggestions(String prefix) {
        String lowered = prefix.toLowerCase(Locale.ROOT);
        return List.of("give", "misiones", "shop", "reload").stream()
            .filter(option -> option.startsWith(lowered))
            .collect(Collectors.toList());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("shop".equalsIgnoreCase(command.getName())) {
            return openShop(sender);
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

        return legacyCommand.onCommand(sender, command, label, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("shop".equalsIgnoreCase(command.getName())) {
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

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " give <item_id> [player] [amount]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " misiones [diarias|semanales|reclamar]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " shop");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " reload");
    }
}
