package dev.linqfy.bigCasares.modules.duels;

/** A duel reserves an inventory throughout a session and until unfinished recovery is settled. */
final class DuelCustodyPolicy {
    private DuelCustodyPolicy() { }

    static boolean owns(boolean session, boolean pending, boolean snapshot, boolean online, boolean restored) {
        boolean confirmed = snapshot && online && restored;
        return session || pending && !confirmed || snapshot && !confirmed;
    }
}
