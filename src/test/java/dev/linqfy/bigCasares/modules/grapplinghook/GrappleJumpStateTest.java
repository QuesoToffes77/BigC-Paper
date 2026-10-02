package dev.linqfy.bigCasares.modules.grapplinghook;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrappleJumpStateTest {

    @Test
    void twoSeparatePressesJumpButHoldingAndThirdPressDoNot() {
        GrappleJumpState state = new GrappleJumpState();

        assertTrue(state.update(true, 2));
        assertFalse(state.update(true, 2), "holding jump must not consume the second jump");
        assertFalse(state.update(false, 2));
        assertTrue(state.update(true, 2));
        assertFalse(state.update(false, 2));
        assertFalse(state.update(true, 2), "a third press must not create another jump");
        assertEquals(2, state.used());
    }
}
