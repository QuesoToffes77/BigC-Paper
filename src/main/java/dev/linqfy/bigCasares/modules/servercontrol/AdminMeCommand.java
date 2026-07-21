package dev.linqfy.bigCasares.modules.servercontrol;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AdminMeCommand implements CommandExecutor {
    private final ServerControlModule module;

    public AdminMeCommand(ServerControlModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo jugadores pueden usar este comando.");
            return true;
        }
        if (!player.isOp()) {
            player.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }
        
        if (module.vanish().isVanished(player)) {
            module.vanish().exit(player, false);
            player.sendMessage("§aVanish desactivado.");
        } else {
            module.vanish().enter(player, false);
            player.sendMessage("§aAhora estás en vanish.");
        }
        return true;
    }
}
