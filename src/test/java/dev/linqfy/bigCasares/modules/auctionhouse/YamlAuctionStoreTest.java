package dev.linqfy.bigCasares.modules.auctionhouse;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlAuctionStoreTest {
    @Test
    void legacyFileWithoutItemReceiptsStillLoads(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("market.yml");
        Files.writeString(file, "next-id: 1\nlistings: {}\nclaims: {}\n");
        var snapshot = new YamlAuctionStore(file, Logger.getAnonymousLogger()).load();
        assertTrue(snapshot.pendingClaims().isEmpty());
        assertTrue(snapshot.pendingListings().isEmpty());
    }

    @Test
    void corruptItemReceiptFailsClosed(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("market.yml");
        Files.writeString(file, "pending-claims:\n  broken-player:\n    receipt-id: invalid\n");
        assertThrows(IllegalStateException.class, () -> new YamlAuctionStore(file, Logger.getAnonymousLogger()).load());
    }

    @Test
    void pendingPurchaseSurvivesDiskRoundTrip(@TempDir Path dir) {
        UUID buyer = UUID.randomUUID();
        Path file = dir.resolve("market.yml");
        AuctionPurchase pending = new AuctionPurchase(buyer, AuctionPurchase.Phase.PAYOUT);
        YamlConfiguration yaml = new YamlConfiguration();
        YamlAuctionStore.writePending(yaml, Map.of(1L, pending));
        try {
            Files.writeString(file, yaml.saveToString());
        } catch (java.io.IOException failure) {
            throw new AssertionError(failure);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file.toFile());
        assertEquals(pending, YamlAuctionStore.readPending(loaded, file).get(1L));
    }

    @Test
    void corruptJournalFailsClosed(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("market.yml");
        Files.writeString(file, "pending-purchases:\n  1:\n    buyer: invalid\n    phase: PAYOUT\n");
        YamlAuctionStore store = new YamlAuctionStore(file, Logger.getAnonymousLogger());
        assertThrows(IllegalStateException.class, store::load);
    }
}
