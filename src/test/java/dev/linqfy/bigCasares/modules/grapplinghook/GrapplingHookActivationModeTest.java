package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.modules.grapplinghook.GrapplingHookActivationMode.InteractionKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every activation mode must react to exactly the input it claims and to no
 * other: a single input matches once, which is what keeps one press from
 * producing more than one session.
 */
class GrapplingHookActivationModeTest {

    @Test
    void rightClickMatchesRightClicksSneakingOrNot() {
        assertTrue(GrapplingHookActivationMode.RIGHT_CLICK.matches(InteractionKind.RIGHT_CLICK, false));
        assertTrue(GrapplingHookActivationMode.RIGHT_CLICK.matches(InteractionKind.RIGHT_CLICK, true));
        assertFalse(GrapplingHookActivationMode.RIGHT_CLICK.matches(InteractionKind.LEFT_CLICK, false));
        assertFalse(GrapplingHookActivationMode.RIGHT_CLICK.matches(InteractionKind.LEFT_CLICK, true));
    }

    @Test
    void leftClickMatchesLeftClicksSneakingOrNot() {
        assertTrue(GrapplingHookActivationMode.LEFT_CLICK.matches(InteractionKind.LEFT_CLICK, false));
        assertTrue(GrapplingHookActivationMode.LEFT_CLICK.matches(InteractionKind.LEFT_CLICK, true));
        assertFalse(GrapplingHookActivationMode.LEFT_CLICK.matches(InteractionKind.RIGHT_CLICK, false));
        assertFalse(GrapplingHookActivationMode.LEFT_CLICK.matches(InteractionKind.RIGHT_CLICK, true));
    }

    @Test
    void sneakRightClickOnlyMatchesWhileSneaking() {
        assertTrue(GrapplingHookActivationMode.SNEAK_RIGHT_CLICK.matches(InteractionKind.RIGHT_CLICK, true));
        assertFalse(GrapplingHookActivationMode.SNEAK_RIGHT_CLICK.matches(InteractionKind.RIGHT_CLICK, false));
        assertFalse(GrapplingHookActivationMode.SNEAK_RIGHT_CLICK.matches(InteractionKind.LEFT_CLICK, true));
    }

    @Test
    void sneakLeftClickOnlyMatchesWhileSneaking() {
        assertTrue(GrapplingHookActivationMode.SNEAK_LEFT_CLICK.matches(InteractionKind.LEFT_CLICK, true));
        assertFalse(GrapplingHookActivationMode.SNEAK_LEFT_CLICK.matches(InteractionKind.LEFT_CLICK, false));
        assertFalse(GrapplingHookActivationMode.SNEAK_LEFT_CLICK.matches(InteractionKind.RIGHT_CLICK, true));
    }

    @Test
    void swapHandsNeverMatchesAClick() {
        assertFalse(GrapplingHookActivationMode.SWAP_HANDS.matches(InteractionKind.RIGHT_CLICK, false));
        assertFalse(GrapplingHookActivationMode.SWAP_HANDS.matches(InteractionKind.RIGHT_CLICK, true));
        assertFalse(GrapplingHookActivationMode.SWAP_HANDS.matches(InteractionKind.LEFT_CLICK, false));
    }

    @Test
    void everyModeParsesFromConfig() {
        for (GrapplingHookActivationMode mode : GrapplingHookActivationMode.values()) {
            assertEquals(mode, GrapplingHookActivationMode.fromConfig(mode.name(), ignored -> {
            }));
        }
        assertEquals(GrapplingHookActivationMode.LEFT_CLICK,
            GrapplingHookActivationMode.fromConfig("  left_click ", ignored -> {
            }));
    }

    @Test
    void unknownModeFallsBackToRightClickWithWarning() {
        List<String> warnings = new ArrayList<>();
        assertEquals(GrapplingHookActivationMode.RIGHT_CLICK,
            GrapplingHookActivationMode.fromConfig("FOO", warnings::add));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("FOO"));
    }

    @Test
    void nullAndBlankFallBackSilently() {
        assertEquals(GrapplingHookActivationMode.RIGHT_CLICK,
            GrapplingHookActivationMode.fromConfig(null, ignored -> {
            }));
        assertEquals(GrapplingHookActivationMode.RIGHT_CLICK,
            GrapplingHookActivationMode.fromConfig("   ", ignored -> {
            }));
    }
}
