package com.copperapple.commands;

import com.copperapple.CopperApplePlugin;
import com.copperapple.items.CopperAppleItem;
import com.copperapple.items.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Comando /copperapple (alias: /ca, /capple)
 *
 * ─── USO ─────────────────────────────────────────────────────────────────────
 *   /copperapple                    → 1 ítem al ejecutor (debe ser jugador)
 *   /copperapple <jugador>          → 1 ítem al jugador indicado
 *   /copperapple <jugador> <cant>   → N ítems al jugador (1–64)
 *
 * ─── PERMISOS ────────────────────────────────────────────────────────────────
 *   copperapple.give        → obtener ítem para sí mismo
 *   copperapple.give.others → dar ítem a otros (implica .give)
 *
 * ─── ANTI-EXPLOIT ────────────────────────────────────────────────────────────
 *   • Cantidad limitada: 1 ≤ amount ≤ 64 (hardcap)
 *   • Inventario lleno: el sobrante se dropea (sin pérdida, sin duplicación)
 *   • El ítem entregado siempre viene del registro (PDC + CMD garantizados)
 */
public class CopperAppleCommand implements CommandExecutor, TabCompleter {

    private final CopperApplePlugin plugin;

    /** Límite máximo de ítems por ejecución de comando (anti-exploit). */
    private static final int MAX_AMOUNT = 64;
    private static final int MIN_AMOUNT = 1;

    public CopperAppleCommand(CopperApplePlugin plugin) {
        this.plugin = plugin;
    }

    // ─── onCommand ────────────────────────────────────────────────────────────

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // ── Sin argumentos: dar al ejecutor (solo jugadores) ──────────────────
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cDesde consola debes especificar un jugador.");
                sender.sendMessage("§7Uso: §f/" + label + " <jugador> [cantidad]");
                return true;
            }
            if (!player.hasPermission("copperapple.give")) {
                player.sendMessage("§cNo tenés permiso para usar este comando.");
                player.sendMessage("§8Permiso requerido: §7copperapple.give");
                return true;
            }
            giveItem(sender, player, 1);
            return true;
        }

        // ── Con argumentos: parsear jugador objetivo ───────────────────────────
        String targetName = args[0];
        boolean targetIsSelf = (sender instanceof Player p) && p.getName().equalsIgnoreCase(targetName);

        // Verificar permiso adecuado
        if (targetIsSelf) {
            if (!sender.hasPermission("copperapple.give")) {
                sender.sendMessage("§cNo tenés permiso para usar este comando.");
                return true;
            }
        } else {
            if (!sender.hasPermission("copperapple.give.others")) {
                sender.sendMessage("§cNo tenés permiso para dar ítems a otros jugadores.");
                sender.sendMessage("§8Permiso requerido: §7copperapple.give.others");
                return true;
            }
        }

        // Buscar jugador objetivo (debe estar conectado)
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage("§cJugador §f" + targetName + " §cno está en línea.");
            return true;
        }

        // Parsear cantidad con validación estricta
        int amount = MIN_AMOUNT;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cCantidad inválida: §f\"" + args[1] + "\"§c. Debe ser un número entero.");
                return true;
            }

            // Validación de rango explícita
            if (amount < MIN_AMOUNT) {
                sender.sendMessage("§cLa cantidad mínima es §f" + MIN_AMOUNT + "§c.");
                return true;
            }
            if (amount > MAX_AMOUNT) {
                sender.sendMessage("§cLa cantidad máxima por comando es §f" + MAX_AMOUNT + "§c.");
                return true;
            }
        }

        giveItem(sender, target, amount);
        return true;
    }

    // ─── Lógica de entrega ────────────────────────────────────────────────────

    /**
     * Entrega el ítem al jugador objetivo.
     *
     * El ítem siempre se obtiene del registro para garantizar que lleva PDC + CMD
     * correctos. NUNCA se instancia un ItemStack nuevo aquí directamente.
     *
     * Manejo de inventario lleno:
     *   addItem() retorna un mapa de los ítems que no cupieron.
     *   Cada sobrante se dropea naturalmente en la ubicación del jugador.
     *   El jugador recibe un aviso por cada stack dropeado.
     */
    private void giveItem(CommandSender sender, Player target, int amount) {
        Optional<CustomItem> customItemOpt = plugin.getItemRegistry()
            .findByModelData(CopperAppleItem.MODEL_DATA);

        if (customItemOpt.isEmpty()) {
            sender.sendMessage("§cError interno: ítem no encontrado en el registro. Revisá los logs.");
            plugin.getLogger().severe(
                "/copperapple: CopperAppleItem no encontrado en el registro. " +
                "Verificar que CustomItemRegistry registró el ítem correctamente."
            );
            return;
        }

        // Construir el ItemStack auténtico (con PDC + CMD)
        ItemStack item = customItemOpt.get().buildItemStack();
        item.setAmount(amount);

        // Intentar agregar al inventario
        // addItem() retorna los ítems que NO cupieron (inventario lleno)
        var leftovers = target.getInventory().addItem(item);

        // Manejar sobrantes: dropear en el suelo junto al jugador
        if (!leftovers.isEmpty()) {
            leftovers.forEach((slot, leftover) -> {
                target.getWorld().dropItemNaturally(target.getLocation(), leftover);
            });
            target.sendMessage("§e⚠ Inventario lleno. §7El/los ítem(s) sobrante(s) fueron dropeados a tus pies.");
        }

        // Mensajes de confirmación
        String itemLabel = amount == 1
            ? "§f1x §6Manzana de Cobre"
            : "§f" + amount + "x §6Manzanas de Cobre";

        if (sender == target) {
            target.sendMessage("§a✔ Recibiste " + itemLabel + "§a.");
        } else {
            target.sendMessage("§a✔ Recibiste " + itemLabel + " §ade §f" + sender.getName() + "§a.");
            sender.sendMessage("§a✔ Entregaste " + itemLabel + " §aa §f" + target.getName() + "§a.");
        }
    }

    // ─── Tab completion ───────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        }
        if (args.length == 2) {
            List<String> suggestions = new ArrayList<>();
            for (String qty : new String[]{"1", "5", "10", "32", "64"}) {
                if (qty.startsWith(args[1])) suggestions.add(qty);
            }
            return suggestions;
        }
        return new ArrayList<>();
    }
}
