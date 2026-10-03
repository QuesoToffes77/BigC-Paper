package dev.linqfy.bigCasares.modules.grapplinghook;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * How the Grappling Hook is activated. Paper/Bukkit cannot detect an arbitrary
 * keybind like "G" without a client mod, so only inputs the vanilla client
 * actually sends are supported: left/right click (optionally while sneaking)
 * and the offhand-swap key (F).
 *
 * {@code SWAP_HANDS} is handled through the vanilla {@code F} key via
 * {@code PlayerSwapHandItemsEvent}; it never matches a click.
 */
public enum GrapplingHookActivationMode {

    /** Right click with the hook (sneaking or not). */
    RIGHT_CLICK,
    /** Left click with the hook (sneaking or not). */
    LEFT_CLICK,
    /** Sneaking + right click. */
    SNEAK_RIGHT_CLICK,
    /** Sneaking + left click. */
    SNEAK_LEFT_CLICK,
    /** Pressing F (vanilla offhand swap) while holding the hook. */
    SWAP_HANDS;

    /**
     * Parses a configured mode name. Unknown, blank or null values fall back
     * to {@link #RIGHT_CLICK} and report a clear warning through
     * {@code warnings}, so a typo can never silently break activation.
     */
    public static GrapplingHookActivationMode fromConfig(String value, Consumer<String> warnings) {
        if (value == null || value.isBlank()) {
            return RIGHT_CLICK;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException invalid) {
            if (warnings != null) {
                warnings.accept("[GrapplingHook] activation.mode '" + value
                    + "' no es un modo valido; usando RIGHT_CLICK. Modos: "
                    + "RIGHT_CLICK, LEFT_CLICK, SNEAK_RIGHT_CLICK, SNEAK_LEFT_CLICK, SWAP_HANDS.");
            }
            return RIGHT_CLICK;
        }
    }

    /**
     * Whether this mode activates for a given interaction input. {@code kind}
     * is the click side; {@code sneaking} is the sneaking state at the moment
     * of the click. {@link #SWAP_HANDS} never matches a click: it is handled
     * by the swap event instead.
     */
    public boolean matches(InteractionKind kind, boolean sneaking) {
        return switch (this) {
            case RIGHT_CLICK -> kind == InteractionKind.RIGHT_CLICK;
            case LEFT_CLICK -> kind == InteractionKind.LEFT_CLICK;
            case SNEAK_RIGHT_CLICK -> sneaking && kind == InteractionKind.RIGHT_CLICK;
            case SNEAK_LEFT_CLICK -> sneaking && kind == InteractionKind.LEFT_CLICK;
            case SWAP_HANDS -> false;
        };
    }

    /** Which mouse button produced a {@code PlayerInteractEvent}. */
    public enum InteractionKind {
        RIGHT_CLICK,
        LEFT_CLICK
    }
}
