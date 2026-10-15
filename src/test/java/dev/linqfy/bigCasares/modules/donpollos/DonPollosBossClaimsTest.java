package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El cobro del premio se guarda en el boss: sobrevive a una instancia nueva (reinicio) antes de la despedida. */
class DonPollosBossClaimsTest {

    private final NamespacedKey defeated = NamespacedKey.fromString("test:derrotado");
    private final NamespacedKey claimed = NamespacedKey.fromString("test:cobrado");
    private final UUID winner = UUID.randomUUID();
    private final UUID bossId = UUID.randomUUID();
    private final java.util.Set<UUID> ledgerSet = new java.util.HashSet<>();
    private final DonPollosBossClaims.Ledger ledger = new DonPollosBossClaims.Ledger() {
        public boolean contains(UUID id) {
            return ledgerSet.contains(id);
        }

        public void add(UUID id) {
            ledgerSet.add(id);
        }

        public void remove(UUID id) {
            ledgerSet.remove(id);
        }
    };
    private final Map<NamespacedKey, Object> store = new HashMap<>();
    private final PersistentDataContainer pdc = (PersistentDataContainer) Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[]{PersistentDataContainer.class}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "has":
                    return store.containsKey(args[0]);
                case "set":
                    store.put((NamespacedKey) args[0], args[2]);
                    return null;
                case "get":
                    return store.get(args[0]);
                default:
                    throw new UnsupportedOperationException(method.getName());
            }
        });

    @Test
    void claimThenRestartThenSecondClaimIsFalse() {
        pdc.set(defeated, PersistentDataType.STRING, winner.toString());
        assertEquals(DonPollosBossClaims.Restore.DEFEATED, DonPollosBossClaims.restore(pdc, defeated, claimed, ledger, bossId));

        assertTrue(DonPollosBossClaims.claim(new DonPollosBoss.Defeat(winner, 0), winner, pdc, claimed, ledger, bossId));

        // reinicio: el Defeat nuevo nace con claimed=false, pero el boss ya tiene el cobro guardado
        DonPollosBoss.Defeat afterRestart = new DonPollosBoss.Defeat(winner, 0);
        assertEquals(DonPollosBossClaims.Restore.CLAIMED, DonPollosBossClaims.restore(pdc, defeated, claimed, ledger, bossId));
        assertFalse(DonPollosBossClaims.claim(afterRestart, winner, pdc, claimed, ledger, bossId));
        assertFalse(afterRestart.claimed);
    }

    @Test
    void aWrongPlayerDoesNotPersistAClaim() {
        pdc.set(defeated, PersistentDataType.STRING, winner.toString());
        assertFalse(DonPollosBossClaims.claim(new DonPollosBoss.Defeat(winner, 0), UUID.randomUUID(), pdc, claimed, ledger, bossId));
        assertFalse(pdc.has(claimed, PersistentDataType.BYTE));
        assertEquals(DonPollosBossClaims.Restore.DEFEATED, DonPollosBossClaims.restore(pdc, defeated, claimed, ledger, bossId));
    }

    @Test
    void aBossThatWasNotDefeatedKeepsFighting() {
        assertEquals(DonPollosBossClaims.Restore.FIGHTING, DonPollosBossClaims.restore(pdc, defeated, claimed, ledger, bossId));
    }

    @Test
    void claimedInLedgerWithoutPdcMarkerRestoresAsClaimed() {
        // crash antes de guardar el chunk: la marca del boss se perdio pero el registro en disco no
        pdc.set(defeated, PersistentDataType.STRING, winner.toString());
        ledger.add(bossId);
        assertFalse(pdc.has(claimed, PersistentDataType.BYTE));
        assertEquals(DonPollosBossClaims.Restore.CLAIMED,
            DonPollosBossClaims.restore(pdc, defeated, claimed, ledger, bossId));
        assertFalse(DonPollosBossClaims.claim(new DonPollosBoss.Defeat(winner, 0), winner, pdc, claimed, ledger, bossId));
    }

    @Test
    void claimWritesTheLedger() {
        pdc.set(defeated, PersistentDataType.STRING, winner.toString());
        assertTrue(DonPollosBossClaims.claim(new DonPollosBoss.Defeat(winner, 0), winner, pdc, claimed, ledger, bossId));
        assertTrue(ledger.contains(bossId));
    }
}
