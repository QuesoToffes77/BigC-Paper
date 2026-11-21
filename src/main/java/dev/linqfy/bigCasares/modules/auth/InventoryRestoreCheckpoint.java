package dev.linqfy.bigCasares.modules.auth;

import java.io.IOException;

/** Keeps the stash authoritative until a fresh player-data checkpoint has been confirmed. */
final class InventoryRestoreCheckpoint {
    @FunctionalInterface
    interface Checkpoint {
        void save() throws IOException;
    }

    private InventoryRestoreCheckpoint() {
    }

    static void restore(boolean alreadyApplied, Runnable apply, Checkpoint checkpoint) {
        if (!alreadyApplied) {
            apply.run();
        }
        try {
            checkpoint.save();
        } catch (IOException failure) {
            throw new IllegalStateException("Could not confirm restored player inventory", failure);
        }
    }
}
