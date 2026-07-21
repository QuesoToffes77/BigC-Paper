package dev.linqfy.bigCasares.modules.discord;

import dev.linqfy.bigCasares.modules.moderation.AuditSink;

import java.util.List;

public interface DiscordGateway extends AuditSink {

    void start();

    void stop();

    boolean isHealthy();

    void updateServerStatus(boolean online);

    void updateVisiblePlayers(List<String> playerNames);

    void sendMinecraftChat(String playerName, String message, String avatarUrl);
    
    void sendPrivateMessage(long userId, String message);
}
