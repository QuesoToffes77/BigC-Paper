package dev.linqfy.bigCasares.modules.servercontrol;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ServerControlCommand implements CommandExecutor {
    private final ServerControlMenu menu;

    public ServerControlCommand(ServerControlMenu menu) {
        this.menu = menu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cEste comando solo puede usarse dentro del juego.");
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage("§cSolo los operadores pueden usar este panel.");
            return true;
        }
        menu.openMain(player);
        return true;
    }
}
