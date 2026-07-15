package dev.linqfy.bigCasares.modules.servercontrol;

import org.bukkit.configuration.ConfigurationSection;

public record ServerControlSettings(String fakeJoinMessage, String fakeLeaveMessage) {

    public static ServerControlSettings load(ConfigurationSection config) {
        return new ServerControlSettings(
            config.getString("server-control-system.vanish.fake-join-message", "§e%player% se unió al servidor"),
            config.getString("server-control-system.vanish.fake-leave-message", "§e%player% salió del servidor")
        );
    }
}
