package dev.linqfy.bigCasares.modules.grapplinghook;

/** Rising-edge jump counter owned by one active grapple session. */
final class GrappleJumpState {

    private boolean held;
    private int used;

    boolean update(boolean pressed, int maximumJumps) {
        boolean triggered = pressed && !held && used < Math.max(0, maximumJumps);
        if (triggered) {
            used++;
        }
        held = pressed;
        return triggered;
    }

    int used() {
        return used;
    }
}
