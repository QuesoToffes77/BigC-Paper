package dev.linqfy.bigCasares.modules.grapplinghook;

/**
 * No-op cosmetic model manager for the Grappling Hook items.
 *
 * <p>The previous implementation attached an animated BetterModel crossbow to a
 * {@link org.bukkit.entity.Marker} entity near the player's hand.  That system
 * spawned entities on every inventory/lifecycle event, teleported them on every
 * player move, and ran animation playback loops — all unnecessary overhead when
 * the item already has a perfectly good 2D texture served by the resource pack.
 *
 * <p>This class is retained as a compile-time stub so that callers
 * ({@link GrapplingHookListener}, {@link GrapplingHookRuntime},
 * {@link GrapplingHookModule}) do not need null-checks everywhere.  Every
 * method is intentionally empty.
 */
public final class GrapplingHookModelManager {

    public void playShoot(@SuppressWarnings("unused") org.bukkit.entity.Player player) {
        // No-op: item uses 2D resource pack texture, no 3D entity to animate.
    }

    public void playReload(@SuppressWarnings("unused") org.bukkit.entity.Player player) {
        // No-op: item uses 2D resource pack texture, no 3D entity to animate.
    }

    public void close() {
        // No-op: nothing to clean up.
    }
}
