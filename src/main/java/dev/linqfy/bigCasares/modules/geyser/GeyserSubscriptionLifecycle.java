package dev.linqfy.bigCasares.modules.geyser;

import java.util.Objects;

final class GeyserSubscriptionLifecycle {
    private final Runnable unregister;
    private boolean closed;

    GeyserSubscriptionLifecycle(Runnable unregister) {
        this.unregister = Objects.requireNonNull(unregister, "unregister");
    }

    synchronized boolean close() {
        if (closed) {
            return false;
        }
        unregister.run();
        closed = true;
        return true;
    }

    synchronized boolean isClosed() {
        return closed;
    }
}
