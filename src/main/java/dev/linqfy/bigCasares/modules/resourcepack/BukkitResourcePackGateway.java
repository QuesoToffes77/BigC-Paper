package dev.linqfy.bigCasares.modules.resourcepack;

import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.net.URI;
import java.util.UUID;

public final class BukkitResourcePackGateway implements ResourcePackGateway {
    private final Server server;

    public BukkitResourcePackGateway(Server server) {
        this.server = server;
    }

    @Override
    public void requestJavaPack(
        UUID playerId,
        UUID packId,
        URI publicUri,
        byte[] sha1,
        String prompt,
        boolean required
    ) {
        Player player = server.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.setResourcePack(packId, publicUri.toString(), sha1, Component.text(prompt), required);
        }
    }
}
