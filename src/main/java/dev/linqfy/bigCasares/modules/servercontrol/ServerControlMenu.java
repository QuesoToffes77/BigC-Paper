package dev.linqfy.bigCasares.modules.servercontrol;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.List;

public final class ServerControlMenu {
    private final JavaPlugin plugin;
    private final ServerControlModule module;

    public ServerControlMenu(JavaPlugin plugin, ServerControlModule module) {
        this.plugin = plugin;
        this.module = module;
    }

    public void openMain(Player player) {
        ControlState state = module.service().state();
        ControlMenuHolder holder = new ControlMenuHolder(ControlMenuHolder.Menu.MAIN);
        Inventory menu = plugin.getServer().createInventory(holder, 27, "Control del servidor");
        holder.inventory(menu);
        menu.setItem(10, item(Material.DIAMOND_SWORD, "§cControl de PvP", List.of(
            "§7Estado efectivo: " + status(module.service().effectivePvp(java.time.Instant.now())),
            "§7Abrir opciones temporizadas"
        )));
        menu.setItem(11, item(Material.END_PORTAL_FRAME, "§dAcceso al End", List.of(status(state.endAccessEnabled()))));
        menu.setItem(12, item(Material.FIREWORK_ROCKET, "§bCohetes con elytra", List.of(status(state.elytraRocketsEnabled()))));
        menu.setItem(13, item(Material.IRON_CHESTPLATE, "§eResistencia global", List.of("§7Nivel: §f" + resistance(state.resistanceLevel()))));
        menu.setItem(15, item(Material.ENDER_EYE, "§5Vanish personal", List.of(status(module.vanish().isVanished(player)))));
        menu.setItem(16, item(Material.BELL, "§6Alertas de staff", List.of(status(module.service().staffAlertsEnabled(player.getUniqueId())))));
        menu.setItem(22, item(Material.BARRIER, "§cCerrar", List.of()));
        player.openInventory(menu);
    }

    public void openPvp(Player player) {
        ControlMenuHolder holder = new ControlMenuHolder(ControlMenuHolder.Menu.PVP);
        Inventory menu = plugin.getServer().createInventory(holder, 27, "Control de PvP");
        holder.inventory(menu);
        menu.setItem(10, item(Material.LIME_DYE, "§aActivar permanentemente", List.of()));
        menu.setItem(11, item(Material.CLOCK, "§aActivar 5 minutos", List.of()));
        menu.setItem(12, item(Material.CLOCK, "§aActivar 10 minutos", List.of()));
        menu.setItem(13, item(Material.CLOCK, "§aActivar 20 minutos", List.of()));
        menu.setItem(15, item(Material.RED_DYE, "§cDesactivar permanentemente", List.of()));
        menu.setItem(16, item(Material.CLOCK, "§cDesactivar 5 minutos", List.of()));
        menu.setItem(17, item(Material.CLOCK, "§cDesactivar 10 minutos", List.of()));
        menu.setItem(18, item(Material.CLOCK, "§cDesactivar 20 minutos", List.of()));
        menu.setItem(22, item(Material.ARROW, "§eVolver", List.of()));
        player.openInventory(menu);
    }

    public void click(Player player, ControlMenuHolder.Menu menu, int slot) {
        if (!player.isOp()) {
            player.closeInventory();
            player.sendMessage("§cSolo los operadores pueden usar este panel.");
            return;
        }
        if (menu == ControlMenuHolder.Menu.MAIN) {
            clickMain(player, slot);
        } else {
            clickPvp(player, slot);
        }
    }

    private void clickMain(Player player, int slot) {
        switch (slot) {
            case 10 -> openPvp(player);
            case 11 -> {
                module.toggleEndAccess(player);
                openMain(player);
            }
            case 12 -> {
                module.toggleElytraRockets(player);
                openMain(player);
            }
            case 13 -> {
                module.cycleResistance(player);
                openMain(player);
            }
            case 15 -> {
                module.vanish().toggle(player);
                openMain(player);
            }
            case 16 -> {
                boolean enabled = module.service().toggleStaffAlerts(player.getUniqueId());
                player.sendMessage("§eAlertas de staff: " + status(enabled));
                module.auditControl(player, "staff-alerts", enabled ? "activadas" : "desactivadas");
                openMain(player);
            }
            case 22 -> player.closeInventory();
            default -> { }
        }
    }

    private void clickPvp(Player player, int slot) {
        switch (slot) {
            case 10 -> module.setPermanentPvp(player, true);
            case 11 -> module.setTimedPvp(player, true, Duration.ofMinutes(5));
            case 12 -> module.setTimedPvp(player, true, Duration.ofMinutes(10));
            case 13 -> module.setTimedPvp(player, true, Duration.ofMinutes(20));
            case 15 -> module.setPermanentPvp(player, false);
            case 16 -> module.setTimedPvp(player, false, Duration.ofMinutes(5));
            case 17 -> module.setTimedPvp(player, false, Duration.ofMinutes(10));
            case 18 -> module.setTimedPvp(player, false, Duration.ofMinutes(20));
            case 22 -> openMain(player);
            default -> { }
        }
        if (slot != 22 && (slot == 10 || slot == 11 || slot == 12 || slot == 13 || slot == 15 || slot == 16 || slot == 17 || slot == 18)) {
            openPvp(player);
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String status(boolean enabled) {
        return enabled ? "§aActivado" : "§cDesactivado";
    }

    private String resistance(ResistanceLevel level) {
        return switch (level) {
            case OFF -> "Desactivada";
            case I -> "I";
            case II -> "II";
        };
    }
}
