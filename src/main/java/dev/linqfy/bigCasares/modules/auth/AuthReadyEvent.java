package dev.linqfy.bigCasares.modules.auth;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired on the server thread after a pending player leaves authentication limbo. */
public final class AuthReadyEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    AuthReadyEvent(Player player) {
        this.player = player;
    }

    public Player player() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
