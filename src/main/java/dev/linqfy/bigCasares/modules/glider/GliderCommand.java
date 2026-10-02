package dev.linqfy.bigCasares.modules.glider;

import dev.linqfy.bigCasares.BigCasares;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Administrative delivery command for the six catalog-backed Gliders. */
final class GliderCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "bigcasares.glider.admin";
    private static final String GIVE_PERMISSION = "bigcasares.glider.give";

    private final BigCasares plugin;

    GliderCommand(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || !"give".equalsIgnoreCase(args[0])) {
            sendUsage(sender, label);
            return true;
        }
        if (!sender.hasPermission(GIVE_PERMISSION) && !sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage("§cNo tenes permiso para entregar Gliders.");
            return true;
        }

        Optional<GliderTier> tier = parseTier(args[1]);
        if (tier.isEmpty()) {
            sender.sendMessage("§cTier invalido. Usa un numero del 1 al 6.");
            return true;
        }
        if (args.length > 4) {
            sendUsage(sender, label);
            return true;
        }

        Player target = args.length >= 3
            ? plugin.getServer().getPlayerExact(args[2])
            : sender instanceof Player player ? player : null;
        if (target == null) {
            sender.sendMessage("§cJugador no encontrado. Desde consola indica un jugador conectado.");
            return true;
        }

        int amount = args.length == 4 ? parseAmount(args[3]) : 1;
        if (amount < 1) {
            sender.sendMessage("§cLa cantidad debe ser un numero entre 1 y 64.");
            return true;
        }

        int dropped = give(target, tier.orElseThrow(), amount);
        sender.sendMessage("§aEntregaste " + amount + "x Glider " + roman(tier.orElseThrow())
            + " a " + target.getName() + ".");
        if (sender != target) {
            target.sendMessage("§aRecibiste " + amount + "x Glider " + roman(tier.orElseThrow()) + ".");
        }
        if (dropped > 0) {
            target.sendMessage("§eInventario lleno: " + dropped + " Glider(s) quedaron en el suelo.");
        }
        return true;
    }

    private int give(Player target, GliderTier tier, int amount) {
        int dropped = 0;
        for (int index = 0; index < amount; index++) {
            ItemStack item = plugin.getCustomItemRegistry().createItemStack(tier.catalogId(), 1);
            var overflow = target.getInventory().addItem(item);
            for (ItemStack leftover : overflow.values()) {
                target.getWorld().dropItemNaturally(target.getLocation(), leftover);
                dropped += leftover.getAmount();
            }
        }
        return dropped;
    }

    static Optional<GliderTier> parseTier(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
            .replace("glider_tier_", "")
            .replace("glider-tier-", "")
            .replace("tier_", "")
            .replace("tier-", "")
            .replace("tier", "");
        try {
            return GliderTier.fromNumber(Integer.parseInt(normalized));
        } catch (NumberFormatException ignored) {
            try {
                return Optional.of(GliderTier.valueOf(normalized.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException invalidTier) {
                return Optional.empty();
            }
        }
    }

    private static int parseAmount(String raw) {
        try {
            int amount = Integer.parseInt(raw);
            return amount >= 1 && amount <= 64 ? amount : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static String roman(GliderTier tier) {
        return tier.name();
    }

    private static void sendUsage(CommandSender sender, String label) {
        sender.sendMessage("§eUso: /" + label + " give <1-6> [jugador] [cantidad]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matches(args[0], List.of("give"));
        }
        if (args.length == 2 && "give".equalsIgnoreCase(args[0])) {
            return matches(args[1], List.of("1", "2", "3", "4", "5", "6"));
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[0])) {
            return matches(args[2], plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList());
        }
        if (args.length == 4 && "give".equalsIgnoreCase(args[0])) {
            return matches(args[3], List.of("1", "2", "4", "8", "16", "32", "64"));
        }
        return List.of();
    }

    private static List<String> matches(String prefix, List<String> values) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }
}
