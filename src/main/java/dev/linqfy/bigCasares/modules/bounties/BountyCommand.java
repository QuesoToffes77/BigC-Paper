package dev.linqfy.bigCasares.modules.bounties;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public final class BountyCommand implements CommandExecutor, TabCompleter {

    private static final int LEADERBOARD_SIZE = 10;

    private final BountyModule module;

    public BountyCommand(BountyModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "bounty" -> showBountyTop(sender, args);
            case "bal", "balance" -> showBalance(sender, args);
            case "withdraw", "widthdraw" -> withdraw(sender, args);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if ((name.equals("bounty") || name.equals("bal") || name.equals("balance")) && args.length == 1) {
            return List.of("top").stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }

    private boolean showBountyTop(CommandSender sender, String[] args) {
        if (args.length > 0 && !"top".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /bounty top");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "--- Top 10 Bounties ---");
        List<BountyPlayerState> top = module.service().topBounties(LEADERBOARD_SIZE);
        if (top.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No hay bounties activas.");
            return true;
        }
        for (int index = 0; index < top.size(); index++) {
            BountyPlayerState state = top.get(index);
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(index + 1) + ". "
                    + ChatColor.WHITE + playerName(state.playerId())
                    + ChatColor.GRAY + " - " + ChatColor.GREEN + module.economy().format(state.activeBounty()));
        }
        return true;
    }

    private boolean showBalance(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.YELLOW + "Uso: /bal top");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "Tu saldo: " + module.economy().format(module.economy().balance(player.getUniqueId())));
            return true;
        }
        if (!"top".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /bal [top]");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "--- Top 10 Balances ---");
        List<BalanceStanding> top = module.topBalances(LEADERBOARD_SIZE);
        if (top.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No hay jugadores registrados.");
            return true;
        }
        for (int index = 0; index < top.size(); index++) {
            BalanceStanding standing = top.get(index);
            sender.sendMessage(ChatColor.YELLOW + String.valueOf(index + 1) + ". "
                    + ChatColor.WHITE + playerName(standing.playerId())
                    + ChatColor.GRAY + " - " + ChatColor.GREEN + module.economy().format(standing.balance()));
        }
        return true;
    }

    private boolean withdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Solo jugadores pueden retirar pagos.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(ChatColor.YELLOW + "Uso: /withdraw <cantidad>");
            return true;
        }
        if (player.getInventory().firstEmpty() < 0) {
            sender.sendMessage(ChatColor.RED + "Necesitas un espacio libre en tu inventario.");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "La cantidad debe ser un número válido.");
            return true;
        }
        if (!Double.isFinite(amount) || amount <= 0.0) {
            sender.sendMessage(ChatColor.RED + "La cantidad debe ser positiva.");
            return true;
        }
        if (module.economy().balance(player.getUniqueId()) < amount) {
            sender.sendMessage(ChatColor.RED + "No tenes saldo suficiente.");
            return true;
        }

        try {
            PaymentVoucher voucher = module.withdrawToVoucher(player.getUniqueId(), amount);
            ItemStack paper = module.paymentPaper().create(voucher, module.economy().format(amount));
            player.getInventory().addItem(paper);
            sender.sendMessage(ChatColor.GREEN + "Retiraste un vale de " + module.economy().format(amount) + ". Entregáselo a quien deba cobrarlo.");
        } catch (RuntimeException ex) {
            module.plugin().getLogger().warning("No se pudo retirar un vale de pago: " + ex.getMessage());
            sender.sendMessage(ChatColor.RED + "No se pudo retirar el vale de pago.");
        }
        return true;
    }

    private String playerName(java.util.UUID playerId) {
        String name = module.plugin().getServer().getOfflinePlayer(playerId).getName();
        return name == null ? playerId.toString() : name;
    }
}
