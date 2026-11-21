package dev.linqfy.bigCasares.modules.forex;

/** Persists one frozen market state before retiring its external-transfer evidence. */
final class ForexSettlement {
    private ForexSettlement() {
    }

    static void persist(ForexStore store, ForexStore.Snapshot snapshot, JournaledForexBank journal) {
        try {
            store.save(snapshot);
            journal.committed();
        } catch (RuntimeException | Error failure) {
            journal.pause();
            throw failure;
        }
    }
}
