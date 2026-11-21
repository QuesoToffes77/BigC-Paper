package dev.linqfy.bigCasares.modules.duels;

/** Keeps the provider registered while a synchronous transfer is still on the stack. */
final class DuelCustodyLifetime {
    private int transfers;
    private Runnable deferred;

    void enter() { transfers++; }

    void leave() {
        if (transfers <= 0) throw new IllegalStateException("Unbalanced custody transfer");
        transfers--;
        if (transfers == 0 && deferred != null) {
            Runnable action = deferred;
            deferred = null;
            action.run();
        }
    }

    void closeWhenIdle(Runnable action) {
        if (deferred != null) throw new IllegalStateException("Custody cleanup already pending");
        if (transfers == 0) action.run();
        else deferred = action;
    }
}
