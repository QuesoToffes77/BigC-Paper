package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class AcidRainBlockErodeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Block block;
    private final AcidRainLevel level;
    private boolean cancelled;

    public AcidRainBlockErodeEvent(Block block, AcidRainLevel level) {
        this.block = block;
        this.level = level;
    }

    public Block getBlock() {
        return block;
    }

    public AcidRainLevel getLevel() {
        return level;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
