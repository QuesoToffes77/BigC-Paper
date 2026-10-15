package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Cobro del premio del boss derrotado guardado en el propio boss (sin Adventure, asi se puede probar): el cobro se
 * persiste ANTES de entregar el premio, asi un reinicio justo despues no deja cobrar de nuevo.
 */
final class DonPollosBossClaims {

    /** Que hacer con un boss que se recarga. */
    enum Restore {
        /** No estaba derrotado: sigue la pelea. */
        FIGHTING,
        /** Derrotado y sin cobrar: vuelve a llorar con el menu listo. */
        DEFEATED,
        /** Derrotado y ya cobrado: se retira, sin ofrecer el premio otra vez. */
        CLAIMED
    }

    /** Registro durable de bosses cobrados (en disco en produccion). */
    interface Ledger {
        boolean contains(UUID bossId);

        void add(UUID bossId);

        void remove(UUID bossId);
    }

    private DonPollosBossClaims() {
    }

    /** Cobrado si lo dice la marca del boss O el registro en disco. */
    static Restore restore(PersistentDataContainer boss, NamespacedKey defeatedKey, NamespacedKey claimedKey,
                           Ledger ledger, UUID bossId) {
        if (!boss.has(defeatedKey, PersistentDataType.STRING) && !ledger.contains(bossId)) {
            return Restore.FIGHTING;
        }
        return isClaimed(boss, claimedKey, ledger, bossId) ? Restore.CLAIMED : Restore.DEFEATED;
    }

    private static boolean isClaimed(PersistentDataContainer boss, NamespacedKey claimedKey, Ledger ledger,
                                     UUID bossId) {
        return boss.has(claimedKey, PersistentDataType.BYTE) || ledger.contains(bossId);
    }

    /**
     * Cobra: false si ya estaba cobrado (en memoria o guardado en el boss) o no le toca a ese jugador; si no, deja
     * guardado el cobro y devuelve true (recien ahi se entrega el premio).
     */
    static boolean claim(DonPollosBoss.Defeat defeat, UUID player, PersistentDataContainer boss,
                         NamespacedKey claimedKey, Ledger ledger, UUID bossId) {
        if (isClaimed(boss, claimedKey, ledger, bossId) || !defeat.tryClaim(player)) {
            return false;
        }
        ledger.add(bossId);
        boss.set(claimedKey, PersistentDataType.BYTE, (byte) 1);
        return true;
    }
}
