package dev.linqfy.bigCasares.modules.discord;

import dev.linqfy.bigCasares.modules.moderation.AuditSink;

import java.util.List;
import java.util.Set;

public interface DiscordGateway extends AuditSink {

    void start();

    void stop();

    boolean isHealthy();

    void updateServerStatus(boolean online);

    void updateVisiblePlayers(List<String> playerNames);

    void sendMinecraftChat(String playerName, String message, String avatarUrl);
    
    void sendPrivateMessage(long userId, String message);

    /** Sends a login-approval DM; false means the gateway cannot currently send it. */
    default boolean sendLoginApproval(long userId, String requestId, String message) {
        return false;
    }

    /** Receives button decisions for login approvals; null removes the handler. */
    default void setLoginApprovalHandler(LoginApprovalHandler handler) {
    }

    @FunctionalInterface
    interface LoginApprovalHandler {
        void onDecision(String requestId, long discordUserId, boolean approved,
                        java.util.function.Consumer<String> reply);
    }

    /** Sends a private team alert; fall back to DMs when no team channel is available. */
    default void sendTeamAlert(String teamKey, String teamTag, Set<Long> memberIds, String message) {
        for (long memberId : memberIds) {
            sendPrivateMessage(memberId, message);
        }
    }

}
