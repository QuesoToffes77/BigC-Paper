package dev.linqfy.bigCasares.modules.netheritequest;

import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.Activation;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.ActivationStatus;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.Progress;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.ProgressStatus;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetheriteQuestTest {

    private static final long HOUR = 60L * 60L * 1000L;
    private final UUID player = UUID.randomUUID();

    @Test
    void exposesStableModuleId() {
        assertEquals("netherite-quests", new NetheriteQuestModule(null).getId());
    }

    @Test
    void firstActivationIsFreeThenEachCostsTwentyMoreLevels() {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());

        assertEquals(0, book.nextActivationCost(player));
        Activation first = book.activate(player, ArmorQuestPiece.HELMET, List.of("a"), 0, 0L);
        assertEquals(new Activation(ActivationStatus.STARTED, 0, "a"), first);

        assertEquals(20, book.nextActivationCost(player));
        assertEquals(ActivationStatus.NOT_ENOUGH_LEVELS,
            book.activate(player, ArmorQuestPiece.CHESTPLATE, List.of("b"), 19, 0L).status());
        assertEquals(new Activation(ActivationStatus.STARTED, 20, "b"),
            book.activate(player, ArmorQuestPiece.CHESTPLATE, List.of("b"), 20, 0L));
        assertEquals(40, book.nextActivationCost(player));
    }

    @Test
    void activationNeedsASoulOfABossNotUsedByAnotherPiece() {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());

        assertEquals(ActivationStatus.NO_SOUL,
            book.activate(player, ArmorQuestPiece.HELMET, List.of(), 100, 0L).status());
        book.activate(player, ArmorQuestPiece.HELMET, List.of("sahur"), 100, 0L);
        assertEquals(ActivationStatus.ALREADY_ACTIVE,
            book.activate(player, ArmorQuestPiece.HELMET, List.of("abyss"), 100, 0L).status());
        assertEquals(ActivationStatus.SOUL_ALREADY_USED,
            book.activate(player, ArmorQuestPiece.BOOTS, List.of("sahur"), 100, 0L).status());
        assertEquals("abyss",
            book.activate(player, ArmorQuestPiece.BOOTS, List.of("sahur", "abyss"), 100, 0L).soulBoss());
    }

    @Test
    void completedPiecesKeepTheirBossButMayReuseItThemselves() {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.LEGGINGS, List.of("sahur"), 0, 0L);
        book.addProgress(player, ArmorQuestPiece.LEGGINGS, 3, 1L);

        assertFalse(book.soulUsable(player, ArmorQuestPiece.HELMET, "sahur"));
        assertTrue(book.soulUsable(player, ArmorQuestPiece.LEGGINGS, "sahur"));
        assertEquals(ActivationStatus.STARTED,
            book.activate(player, ArmorQuestPiece.LEGGINGS, List.of("sahur"), 20, 2L).status());
    }

    @Test
    void activationReceiptSurvivesRestartAndFreezesQuestProgress(@TempDir Path directory) throws Exception {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.LEGGINGS, List.of("sahur"), 0, 0L);
        book.beginActivationReceipt(player, ArmorQuestPiece.LEGGINGS, "sahur", 0, "before", 0, 0f, 0);
        NetheriteQuestStore store = new NetheriteQuestStore(directory.resolve("quests.yml"));
        store.save(book);
        NetheriteQuestBook restored = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        store.loadInto(restored);
        assertTrue(restored.activationPending(player));
        assertTrue(restored.addProgress(player, ArmorQuestPiece.LEGGINGS, 3, 1L).isEmpty());
        assertTrue(restored.pendingArmor(player).isEmpty());
        assertEquals(ActivationStatus.PENDING_REVIEW,
            restored.activate(player, ArmorQuestPiece.BOOTS, List.of("another"), 100, 2L).status());
        var receipt = restored.activationReceipt(player).orElseThrow();
        restored.recordActivationOutcome(player, "after", 0, 0f, 0);
        store.save(restored);
        NetheriteQuestBook reloadedOutcome = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        store.loadInto(reloadedOutcome);
        assertEquals("after", reloadedOutcome.activationReceipt(player).orElseThrow().inventoryAfter());
        assertFalse(restored.finishActivation(player, UUID.randomUUID().toString()));
        assertTrue(restored.finishActivation(player, receipt.receiptId()));
        assertFalse(restored.soulUsable(player, ArmorQuestPiece.BOOTS, "sahur"));
        assertEquals(20, restored.nextActivationCost(player));
        assertEquals(ProgressStatus.COMPLETED,
            restored.addProgress(player, ArmorQuestPiece.LEGGINGS, 3, 1L).orElseThrow().status());
    }

    @Test
    void soulDeliveryReceiptRemainsPendingAfterRestart(@TempDir Path directory) throws Exception {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.addPendingSoul(player, "sahur", "Sahur", 2);
        String receipt = book.beginSoulDelivery(player, "sahur", 1, "before").orElseThrow();
        book.recordSoulOutcome(player, "after");
        NetheriteQuestStore store = new NetheriteQuestStore(directory.resolve("quests.yml"));
        store.save(book);
        NetheriteQuestBook restored = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        store.loadInto(restored);
        assertEquals("before", restored.soulDelivery(player).orElseThrow().inventoryBefore());
        assertEquals("after", restored.soulDelivery(player).orElseThrow().inventoryAfter());
        assertTrue(restored.beginSoulDelivery(player, "sahur", 2).isEmpty());
        assertEquals(2, restored.view(player).pendingSouls().get("sahur").amount());
        assertFalse(restored.resolveSoulDelivery(player, UUID.randomUUID().toString(), true));
        assertTrue(restored.resolveSoulDelivery(player, receipt, true));
        assertEquals(1, restored.view(player).pendingSouls().get("sahur").amount());
    }

    @Test
    void legacyQuestDataWithoutReceiptsStillLoads(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("quests.yml");
        Files.writeString(file, "players:\n  " + player + ":\n    activations: 1\n    souls:\n      leggings: sahur\n");
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        new NetheriteQuestStore(file).loadInto(book);
        assertTrue(book.pendingArmor(player).isEmpty());
        assertTrue(book.armorDelivery(player).isEmpty());
        assertFalse(book.activationPending(player));
        assertTrue(book.soulDelivery(player).isEmpty());
        assertEquals(20, book.nextActivationCost(player));
    }

    @Test
    void malformedPendingReceiptDoesNotLoadAsEmpty(@TempDir Path directory) throws Exception {
        Path file = directory.resolve("quests.yml");
        Files.writeString(file, "players:\n  " + player + ":\n    soul-delivery:\n      boss: sahur\n      amount: 1\n      receipt: invalid\n");
        assertThrows(IOException.class,
            () -> new NetheriteQuestStore(file).loadInto(new NetheriteQuestBook(NetheriteQuestSettings.defaults())));
    }

    @Test
    void completedArmorRemainsPendingAfterRestart(@TempDir Path directory) throws Exception {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.LEGGINGS, List.of("sahur"), 0, 0L);
        book.addProgress(player, ArmorQuestPiece.LEGGINGS, 3, 1L);
        NetheriteQuestStore store = new NetheriteQuestStore(directory.resolve("quests.yml"));
        store.save(book);
        NetheriteQuestBook restored = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        store.loadInto(restored);
        assertEquals(1, restored.pendingArmor(player).get(ArmorQuestPiece.LEGGINGS));
    }

    @Test
    void armorDeliveryReceiptSurvivesRestartAndRequiresExactAcknowledgement(@TempDir Path directory) throws Exception {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.LEGGINGS, List.of("sahur"), 0, 0L);
        book.addProgress(player, ArmorQuestPiece.LEGGINGS, 3, 1L);
        var delivery = book.beginArmorDelivery(player, ArmorQuestPiece.LEGGINGS, "before").orElseThrow();
        book.recordArmorOutcome(player, "after");
        NetheriteQuestStore store = new NetheriteQuestStore(directory.resolve("quests.yml"));
        store.save(book);
        NetheriteQuestBook restored = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        store.loadInto(restored);
        assertEquals(delivery.receiptId(), restored.armorDelivery(player).orElseThrow().receiptId());
        assertEquals("before", restored.armorDelivery(player).orElseThrow().inventoryBefore());
        assertEquals("after", restored.armorDelivery(player).orElseThrow().inventoryAfter());
        assertTrue(restored.beginArmorDelivery(player, ArmorQuestPiece.LEGGINGS).isEmpty());
        assertFalse(restored.finishArmorDelivery(player, UUID.randomUUID().toString()));
        assertTrue(restored.finishArmorDelivery(player, delivery.receiptId()));
        assertTrue(restored.pendingArmor(player).isEmpty());
    }

    @Test
    void manualResolutionRequiresExactReceiptAndDoesNotDropUnpaidRewards() {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.LEGGINGS, List.of("sahur"), 0, 0L);
        book.beginActivationReceipt(player, ArmorQuestPiece.LEGGINGS, "sahur", 0, "before", 0, 0f, 0);
        String activationId = book.activationReceipt(player).orElseThrow().receiptId();
        assertFalse(book.resolveActivation(player, UUID.randomUUID().toString(), false));
        assertTrue(book.resolveActivation(player, activationId, false));
        assertEquals(0, book.nextActivationCost(player));
        assertFalse(book.hasActive(player, ArmorQuestPiece.LEGGINGS));
        book.activate(player, ArmorQuestPiece.LEGGINGS, List.of("sahur"), 0, 0L);
        book.addProgress(player, ArmorQuestPiece.LEGGINGS, 3, 1L);
        String armorId = book.beginArmorDelivery(player, ArmorQuestPiece.LEGGINGS).orElseThrow().receiptId();
        assertFalse(book.resolveArmorDelivery(player, UUID.randomUUID().toString(), false));
        assertTrue(book.resolveArmorDelivery(player, armorId, false));
        assertEquals(1, book.pendingArmor(player).get(ArmorQuestPiece.LEGGINGS));
        assertTrue(book.armorDelivery(player).isEmpty());
    }

    @Test
    void progressCompletesAtTheTarget() {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.LEGGINGS, List.of("sahur"), 0, 0L);

        assertEquals(ProgressStatus.ADVANCED,
            book.addProgress(player, ArmorQuestPiece.LEGGINGS, 1, 1L).orElseThrow().status());
        assertTrue(book.addProgress(player, ArmorQuestPiece.HELMET, 1, 1L).isEmpty(), "not active");
        book.addProgress(player, ArmorQuestPiece.LEGGINGS, 1, 1L);
        Progress done = book.addProgress(player, ArmorQuestPiece.LEGGINGS, 1, 1L).orElseThrow();

        assertEquals(ProgressStatus.COMPLETED, done.status());
        assertEquals(3, done.target());
        assertFalse(book.hasActive(player, ArmorQuestPiece.LEGGINGS));
        assertEquals("sahur", book.view(player).souls().get(ArmorQuestPiece.LEGGINGS));
        assertEquals(1, book.view(player).completions().get(ArmorQuestPiece.LEGGINGS));
    }

    @Test
    void questsExpireAfterTwentyFourHours() {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.BOOTS, List.of("sahur"), 0, 0L);

        assertEquals(ProgressStatus.ADVANCED,
            book.addProgress(player, ArmorQuestPiece.BOOTS, 5.5, 24 * HOUR - 1).orElseThrow().status());
        assertEquals(ProgressStatus.EXPIRED,
            book.addProgress(player, ArmorQuestPiece.BOOTS, 5.5, 24 * HOUR).orElseThrow().status());
        assertFalse(book.hasActive(player, ArmorQuestPiece.BOOTS));
        assertTrue(book.soulUsable(player, ArmorQuestPiece.HELMET, "sahur"), "expired soul is not bound");

        book.activate(player, ArmorQuestPiece.CHESTPLATE, List.of("abyss"), 20, 0L);
        assertEquals(List.of(new NetheriteQuestBook.Expired(player, ArmorQuestPiece.CHESTPLATE)),
            book.expireAll(25 * HOUR));
    }

    @Test
    void cancelledActivationIsNotCharged() {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.HELMET, List.of("sahur"), 0, 0L);
        book.cancelActivation(player, ArmorQuestPiece.HELMET);

        assertEquals(0, book.nextActivationCost(player));
        assertFalse(book.hasActive(player, ArmorQuestPiece.HELMET));
    }

    @Test
    void pendingSoulsAccumulateUntilDrained() {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.addPendingSoul(player, "sahur", "Sahur", 1);
        book.addPendingSoul(player, "sahur", "Sahur", 1);

        assertEquals(Map.of("sahur", new NetheriteQuestBook.PendingSoul("Sahur", 2)), book.drainPendingSouls(player));
        assertTrue(book.drainPendingSouls(player).isEmpty());
    }

    @Test
    void storeRoundTripsEveryField(@TempDir Path folder) throws Exception {
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.LEGGINGS, List.of("sahur"), 0, 0L);
        book.addProgress(player, ArmorQuestPiece.LEGGINGS, 3, 1L);
        book.activate(player, ArmorQuestPiece.BOOTS, List.of("abyss"), 20, 10L);
        book.addProgress(player, ArmorQuestPiece.BOOTS, 123.25, 11L);
        book.addPendingSoul(player, "mutango", "Mutango", 2);

        NetheriteQuestStore store = new NetheriteQuestStore(folder.resolve("data").resolve("quests.yml"));
        store.save(book);
        NetheriteQuestBook loaded = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        store.loadInto(loaded);

        assertEquals(book.view(player), loaded.view(player));
        assertEquals(40, loaded.nextActivationCost(player));
    }

    @Test
    void failedReplacementPreservesExistingQuestFile(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("quests.yml");
        String previous = "players: {}\n";
        Files.writeString(file, previous);
        Files.createDirectory(folder.resolve("quests.yml.tmp"));
        NetheriteQuestStore store = new NetheriteQuestStore(file);
        assertThrows(IOException.class, () -> store.save(new NetheriteQuestBook(NetheriteQuestSettings.defaults())));
        assertEquals(previous, Files.readString(file));
    }

    @Test
    void savedQuestStateCanReplaceAnExistingLegacyFile(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("quests.yml");
        Files.writeString(file, "players: {}\n");
        NetheriteQuestStore store = new NetheriteQuestStore(file);
        NetheriteQuestBook book = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        book.activate(player, ArmorQuestPiece.HELMET, List.of("mutango"), 0, 0L);
        store.save(book);
        NetheriteQuestBook restored = new NetheriteQuestBook(NetheriteQuestSettings.defaults());
        store.loadInto(restored);
        assertEquals(book.view(player), restored.view(player));
    }

    @Test
    void settingsLoadFromConfigAndRejectNonsense() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("netherite-quests.duration-hours", 12);
        yaml.set("netherite-quests.level-cost-step", -5);
        yaml.set("netherite-quests.targets.peto", 250);
        yaml.set("netherite-quests.targets.botas", 0);
        NetheriteQuestSettings settings = NetheriteQuestSettings.load(yaml);

        assertEquals(12L, settings.durationHours());
        assertEquals(20, settings.levelCostStep());
        assertEquals(250, settings.target(ArmorQuestPiece.CHESTPLATE));
        assertEquals(40_000, settings.target(ArmorQuestPiece.BOOTS));
        assertEquals(3, settings.target(ArmorQuestPiece.HELMET));
        assertThrows(IllegalArgumentException.class,
            () -> new NetheriteQuestSettings(0L, 20, true, true, 10.0, Map.of()));
    }

    @Test
    void piecesMatchTheirQuests() {
        assertEquals("elder_guardian", ArmorQuestPiece.HELMET.killTarget().orElseThrow());
        assertEquals("creeper", ArmorQuestPiece.CHESTPLATE.killTarget().orElseThrow());
        assertEquals("warden", ArmorQuestPiece.LEGGINGS.killTarget().orElseThrow());
        assertTrue(ArmorQuestPiece.BOOTS.killTarget().isEmpty());
        assertEquals(ArmorQuestPiece.LEGGINGS, ArmorQuestPiece.parse(" Pantalones ").orElseThrow());
        assertTrue(ArmorQuestPiece.parse("guantes").isEmpty());
    }

    @Test
    void soulsGoToTheThreeHighestDamageDealers() {
        UUID first = new UUID(0, 1);
        UUID second = new UUID(0, 2);
        UUID third = new UUID(0, 3);
        UUID fourth = new UUID(0, 4);
        Map<UUID, Double> damage = new java.util.HashMap<>(Map.of(
            fourth, 10.0, second, 80.0, first, 120.0, third, 40.0, UUID.randomUUID(), 0.0));
        damage.put(UUID.randomUUID(), Double.NaN);

        assertEquals(List.of(first, second, third), BossSoulRanking.topThree(damage));
        assertEquals(List.of(second), BossSoulRanking.topThree(Map.of(second, 1.0)));
        assertTrue(BossSoulRanking.topThree(Map.of()).isEmpty());
    }

    @Test
    void walkingIgnoresTeleports() {
        assertEquals(5.0, WalkDistance.horizontal(0, 0, 3, 4, 10.0), 1e-9);
        assertEquals(0.0, WalkDistance.horizontal(0, 0, 30, 40, 10.0));
        assertEquals(0.0, WalkDistance.horizontal(1, 1, 1, 1, 10.0));
    }
}
