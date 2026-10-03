package dev.linqfy.bigCasares.modules.grapplinghook;

/**
 * Pure decision table for what a Grappling Hook ray may attach to. The
 * Bukkit-side listener classifies a hit {@link org.bukkit.entity.Entity} into
 * a {@link Kind}; the allowed/denied rules live here so they are unit-testable
 * without a server.
 */
public final class GrappleTargetRules {

    public enum Kind {
        /** The shooter itself: never a target. */
        SELF,
        /** Another player: only when {@code allowPlayers} is enabled. */
        PLAYER,
        /** Display/interaction entities (chain, hook, holograms, bosses): never. */
        DISPLAY,
        /** Projectiles (including any hook projectile): never. */
        PROJECTILE,
        /** Invisible {@code Marker} entities: never. */
        MARKER,
        /** Armor stands (NPC decoration, our own weapon anchor): never. */
        ARMOR_STAND,
        /** Any other living creature: allowed by default. */
        LIVING,
        /** Any remaining entity (boats, minecarts, items...): allowed. */
        OTHER
    }

    private GrappleTargetRules() {
    }

    /**
     * @param kind          the classified hit
     * @param valid         {@code true} if the entity is still valid (loaded/attached)
     * @param dead          {@code true} if the entity is dead or should be ignored
     * @param allowPlayers  whether {@link Kind#PLAYER} may be a target
     */
    public static boolean isAllowed(Kind kind, boolean valid, boolean dead, boolean allowPlayers) {
        if (kind == null || !valid || dead) {
            return false;
        }
        return switch (kind) {
            case SELF, DISPLAY, PROJECTILE, MARKER, ARMOR_STAND -> false;
            case PLAYER -> allowPlayers;
            case LIVING, OTHER -> true;
        };
    }
}
