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
            player.addResourcePack(packId, publicUri.toString(), sha1, prompt, required);
        }
    }

    @Override
    public void removeJavaPack(UUID playerId, UUID packId) {
        Player player = server.getPlayer(playerId);
        if (player != null && player.isOnline()) player.removeResourcePack(packId);
    }
}
