package dev.linqfy.bigCasares.modules.netheritequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure quest rules: activation cost, boss soul usage, progress, completion and expiry.
 *
 * <p>Each activation consumes one boss soul. A boss can back only one armor piece per player, so the full set
 * needs souls of four different bosses; redoing a piece may reuse the boss that piece already used.
 */
public final class NetheriteQuestBook {

    private final NetheriteQuestSettings settings;
    private final Map<UUID, PlayerQuests> players = new LinkedHashMap<>();

    public NetheriteQuestBook(NetheriteQuestSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public NetheriteQuestSettings settings() {
        return settings;
    }

    public Map<ArmorQuestPiece, Integer> pendingArmor(UUID playerId) {
        return Collections.unmodifiableMap(new EnumMap<>(state(playerId).pendingArmor));
    }

    /** Levels the next activation of this player costs: free the first time, then {@code step} more each time. */
    public int nextActivationCost(UUID playerId) {
        return (int) Math.min(Integer.MAX_VALUE, (long) state(playerId).activations * settings.levelCostStep());
    }

    public Activation activate(UUID playerId, ArmorQuestPiece piece, Collection<String> heldSoulBosses,
                               int playerLevels, long now) {
        Objects.requireNonNull(piece, "piece");
        PlayerQuests state = state(playerId);
        if (state.activationReceipt != null) return Activation.refused(ActivationStatus.PENDING_REVIEW);
        expire(state, now);
        if (state.active.containsKey(piece)) {
            return Activation.refused(ActivationStatus.ALREADY_ACTIVE);
        }
        if (heldSoulBosses == null || heldSoulBosses.isEmpty()) {
            return Activation.refused(ActivationStatus.NO_SOUL);
        }
        Optional<String> soul = heldSoulBosses.stream()
            .filter(Objects::nonNull)
            .filter(boss -> soulUsable(state, piece, boss))
            .findFirst();
        if (soul.isEmpty()) {
            return Activation.refused(ActivationStatus.SOUL_ALREADY_USED);
        }
        int cost = nextActivationCost(playerId);
        if (playerLevels < cost) {
            return new Activation(ActivationStatus.NOT_ENOUGH_LEVELS, cost, null);
        }
        state.activations++;
        state.active.put(piece, new ActiveQuest(now, soul.get(), 0.0));
        return new Activation(ActivationStatus.STARTED, cost, soul.get());
    }

    /** Undoes an activation that could not be paid for. */
    public void cancelActivation(UUID playerId, ArmorQuestPiece piece) {
        PlayerQuests state = players.get(playerId);
        if (state != null && state.activationReceipt != null) throw new IllegalStateException("Activation receipt is pending");
        if (state != null && state.active.remove(piece) != null) {
            state.activations = Math.max(0, state.activations - 1);
        }
    }

    public boolean soulUsable(UUID playerId, ArmorQuestPiece piece, String bossId) {
        return soulUsable(state(playerId), piece, bossId);
    }

    public boolean hasActive(UUID playerId, ArmorQuestPiece piece) {
        PlayerQuests state = players.get(playerId);
        return state != null && state.active.containsKey(piece)
            && (state.activationReceipt == null || state.activationReceipt.piece() != piece);
    }

    public void beginActivationReceipt(UUID playerId, ArmorQuestPiece piece, String soul, int cost,
                                       String inventoryBefore, int levelBefore, float expBefore, int totalBefore) {
        PlayerQuests state = state(playerId);
        ActiveQuest quest = state.active.get(piece);
        if (state.activationReceipt != null || quest == null || !quest.soulBoss().equals(soul)) {
            throw new IllegalStateException("Activation does not match the quest");
        }
        state.activationReceipt = new ActivationReceipt(piece, soul, cost, UUID.randomUUID().toString(),
            inventoryBefore, levelBefore, expBefore, totalBefore, null, 0, 0, 0);
    }

    public boolean activationPending(UUID playerId) { return state(playerId).activationReceipt != null; }

    public Optional<ActivationReceipt> activationReceipt(UUID playerId) {
        return Optional.ofNullable(state(playerId).activationReceipt);
    }

    public void recordActivationOutcome(UUID playerId, String inventoryAfter, int levelAfter, float expAfter, int totalAfter) {
        PlayerQuests state = state(playerId);
        ActivationReceipt receipt = Objects.requireNonNull(state.activationReceipt, "activation receipt");
        state.activationReceipt = new ActivationReceipt(receipt.piece(), receipt.soulBoss(), receipt.cost(), receipt.receiptId(),
            receipt.inventoryBefore(), receipt.levelBefore(), receipt.expBefore(), receipt.totalBefore(),
            Objects.requireNonNull(inventoryAfter), levelAfter, expAfter, totalAfter);
    }

    public boolean finishActivation(UUID playerId, String receiptId) {
        PlayerQuests state = state(playerId);
        ActivationReceipt receipt = state.activationReceipt;
        if (receipt == null || receipt.inventoryAfter() == null || !receipt.receiptId().equals(receiptId)) return false;
        state.activationReceipt = null;
        return true;
    }

    public boolean resolveActivation(UUID playerId, String receiptId, boolean paid) {
        PlayerQuests state = state(playerId);
        ActivationReceipt receipt = state.activationReceipt;
        if (receipt == null || !receipt.receiptId().equals(receiptId)) return false;
        state.activationReceipt = null;
        if (!paid) cancelActivation(playerId, receipt.piece());
        return true;
    }

    public boolean resolveArmorDelivery(UUID playerId, String receiptId, boolean delivered) {
        PlayerQuests state = state(playerId);
        ArmorDelivery receipt = state.armorDelivery;
        if (receipt == null || !receipt.receiptId().equals(receiptId)) return false;
        if (delivered) return finishArmorDelivery(playerId, receiptId);
        state.armorDelivery = null;
        return true;
    }

    boolean cancelUnpaidActivation(UUID playerId, String receiptId) {
        PlayerQuests state = state(playerId);
        ActivationReceipt receipt = state.activationReceipt;
        if (receipt == null || !receipt.receiptId().equals(receiptId)) return false;
        if (receipt.inventoryAfter() != null) throw new IllegalStateException("Activation may already have been paid");
        state.activationReceipt = null;
        cancelActivation(playerId, receipt.piece());
        return true;
    }

    /** Adds progress to an active quest; completing it frees the slot and binds its soul's boss to the piece. */
    public Optional<Progress> addProgress(UUID playerId, ArmorQuestPiece piece, double amount, long now) {
        PlayerQuests state = players.get(playerId);
        if (state == null || !(amount > 0.0)) {
            return Optional.empty();
        }
        if (state.activationReceipt != null && state.activationReceipt.piece() == piece) return Optional.empty();
        ActiveQuest quest = state.active.get(piece);
        if (quest == null) {
            return Optional.empty();
        }
        if (isExpired(quest, now)) {
            state.active.remove(piece);
            return Optional.of(new Progress(piece, quest.progress(), quest.progress(), settings.target(piece),
                ProgressStatus.EXPIRED));
        }
        int target = settings.target(piece);
        double updated = Math.min(target, quest.progress() + amount);
        if (updated >= target) {
            state.active.remove(piece);
            state.souls.put(piece, quest.soulBoss());
            state.completions.merge(piece, 1, Integer::sum);
            state.pendingArmor.merge(piece, 1, Integer::sum);
            return Optional.of(new Progress(piece, quest.progress(), updated, target, ProgressStatus.COMPLETED));
        }
        state.active.put(piece, quest.withProgress(updated));
        return Optional.of(new Progress(piece, quest.progress(), updated, target, ProgressStatus.ADVANCED));
    }

    /** Removes every quest past its deadline. */
    public List<Expired> expireAll(long now) {
        List<Expired> expired = new ArrayList<>();
        players.forEach((playerId, state) -> expire(state, now).forEach(piece ->
            expired.add(new Expired(playerId, piece))));
        return expired;
    }

    List<ArmorQuestPiece> expireFor(UUID playerId, long now) {
        PlayerQuests state = players.get(playerId);
        return state == null ? List.of() : expire(state, now);
    }

    public long remainingMillis(ActiveQuest quest, long now) {
        return Math.max(0L, quest.startedAt() + settings.durationMillis() - now);
    }

    public void resetPlayer(UUID playerId) {
        PlayerQuests state = players.get(playerId);
        if (state != null) {
            if (state.activationReceipt != null) throw new IllegalStateException("Activation receipt is pending");
            state.activations = 0;
            state.active.clear();
            state.souls.clear();
            state.completions.clear();
        }
    }

    public void addPendingSoul(UUID playerId, String bossId, String bossName, int amount) {
        if (amount < 1) {
            return;
        }
        state(playerId).pendingSouls.merge(bossId, new PendingSoul(bossName, amount),
            (old, added) -> new PendingSoul(added.bossName(), old.amount() + added.amount()));
    }

    public Optional<String> beginSoulDelivery(UUID playerId, String bossId, int amount) {
        return beginSoulDelivery(playerId, bossId, amount, "");
    }

    public Optional<String> beginSoulDelivery(UUID playerId, String bossId, int amount, String inventoryBefore) {
        PlayerQuests state = state(playerId);
        PendingSoul pending = state.pendingSouls.get(bossId);
        if (state.soulDelivery != null || pending == null || amount < 1 || amount > pending.amount()) return Optional.empty();
        state.soulDelivery = new SoulDelivery(bossId, amount, UUID.randomUUID().toString(), inventoryBefore, null);
        return Optional.of(state.soulDelivery.receiptId());
    }

    public void recordSoulOutcome(UUID playerId, String inventoryAfter) {
        PlayerQuests state = state(playerId);
        SoulDelivery receipt = Objects.requireNonNull(state.soulDelivery, "soul receipt");
        state.soulDelivery = new SoulDelivery(receipt.bossId(), receipt.amount(), receipt.receiptId(),
            receipt.inventoryBefore(), Objects.requireNonNull(inventoryAfter));
    }

    public Optional<SoulDelivery> soulDelivery(UUID playerId) {
        return Optional.ofNullable(state(playerId).soulDelivery);
    }

    public boolean resolveSoulDelivery(UUID playerId, String receiptId, boolean delivered) {
        PlayerQuests state = state(playerId);
        SoulDelivery receipt = state.soulDelivery;
        if (receipt == null || !receipt.receiptId().equals(receiptId)) return false;
        PendingSoul pending = state.pendingSouls.get(receipt.bossId());
        if (pending == null || pending.amount() < receipt.amount()) throw new IllegalStateException("Soul receipt without reward");
        if (delivered) {
            if (pending.amount() == receipt.amount()) state.pendingSouls.remove(receipt.bossId());
            else state.pendingSouls.put(receipt.bossId(), new PendingSoul(pending.bossName(), pending.amount() - receipt.amount()));
        }
        state.soulDelivery = null;
        return true;
    }

    /** Takes the souls kept while the player was offline. */
    public Map<String, PendingSoul> drainPendingSouls(UUID playerId) {
        PlayerQuests state = players.get(playerId);
        if (state == null || state.pendingSouls.isEmpty() || state.soulDelivery != null) {
            return Map.of();
        }
        Map<String, PendingSoul> drained = new LinkedHashMap<>(state.pendingSouls);
        state.pendingSouls.clear();
        return drained;
    }

    public PlayerView view(UUID playerId) {
        PlayerQuests state = state(playerId);
        return new PlayerView(state.activations, Collections.unmodifiableMap(new EnumMap<>(state.active)),
            Collections.unmodifiableMap(new EnumMap<>(state.souls)),
            Collections.unmodifiableMap(new EnumMap<>(state.completions)),
            Collections.unmodifiableMap(new LinkedHashMap<>(state.pendingSouls)),
            Collections.unmodifiableMap(new EnumMap<>(state.pendingArmor)), state.armorDelivery, state.activationReceipt,
            state.soulDelivery);
    }

    public Map<UUID, PlayerView> snapshot() {
        Map<UUID, PlayerView> snapshot = new LinkedHashMap<>();
        players.keySet().forEach(playerId -> snapshot.put(playerId, view(playerId)));
        return snapshot;
    }

    /** Replaces a player's state from storage. */
    public void restore(UUID playerId, PlayerView view) {
        PlayerQuests state = new PlayerQuests();
        state.activations = Math.max(0, view.activations());
        state.active.putAll(view.active());
        state.souls.putAll(view.souls());
        state.completions.putAll(view.completions());
        state.pendingSouls.putAll(view.pendingSouls());
        state.pendingArmor.putAll(view.pendingArmor());
        state.armorDelivery = view.armorDelivery();
        state.activationReceipt = view.activationReceipt();
        state.soulDelivery = view.soulDelivery();
        players.put(playerId, state);
    }

    public Optional<ArmorDelivery> armorDelivery(UUID playerId) {
        return Optional.ofNullable(state(playerId).armorDelivery);
    }

    public Optional<ArmorDelivery> beginArmorDelivery(UUID playerId, ArmorQuestPiece piece) {
        return beginArmorDelivery(playerId, piece, "");
    }

    public Optional<ArmorDelivery> beginArmorDelivery(UUID playerId, ArmorQuestPiece piece, String inventoryBefore) {
        PlayerQuests state = state(playerId);
        if (state.armorDelivery != null || state.pendingArmor.getOrDefault(piece, 0) < 1) return Optional.empty();
        state.armorDelivery = new ArmorDelivery(piece, UUID.randomUUID().toString(), inventoryBefore, null);
        return Optional.of(state.armorDelivery);
    }

    public void recordArmorOutcome(UUID playerId, String inventoryAfter) {
        PlayerQuests state = state(playerId);
        ArmorDelivery receipt = Objects.requireNonNull(state.armorDelivery, "armor receipt");
        state.armorDelivery = new ArmorDelivery(receipt.piece(), receipt.receiptId(), receipt.inventoryBefore(),
            Objects.requireNonNull(inventoryAfter));
    }

    public boolean finishArmorDelivery(UUID playerId, String receipt) {
        PlayerQuests state = state(playerId);
        ArmorDelivery delivery = state.armorDelivery;
        if (delivery == null || !delivery.receiptId().equals(receipt)) return false;
        int count = state.pendingArmor.getOrDefault(delivery.piece(), 0);
        if (count < 1) throw new IllegalStateException("Armor receipt without pending reward");
        if (count == 1) state.pendingArmor.remove(delivery.piece());
        else state.pendingArmor.put(delivery.piece(), count - 1);
        state.armorDelivery = null;
        return true;
    }

    private boolean soulUsable(PlayerQuests state, ArmorQuestPiece piece, String bossId) {
        for (ArmorQuestPiece other : ArmorQuestPiece.values()) {
            if (other == piece) {
                continue;
            }
            ActiveQuest active = state.active.get(other);
            if (bossId.equals(state.souls.get(other)) || (active != null && bossId.equals(active.soulBoss()))) {
                return false;
            }
        }
        return true;
    }

    private List<ArmorQuestPiece> expire(PlayerQuests state, long now) {
        List<ArmorQuestPiece> expired = new ArrayList<>();
        state.active.entrySet().removeIf(entry -> {
            if (state.activationReceipt != null && state.activationReceipt.piece() == entry.getKey()) return false;
            if (isExpired(entry.getValue(), now)) {
                expired.add(entry.getKey());
                return true;
            }
            return false;
        });
        return expired;
    }

    private boolean isExpired(ActiveQuest quest, long now) {
        return now >= quest.startedAt() + settings.durationMillis();
    }

    private PlayerQuests state(UUID playerId) {
        return players.computeIfAbsent(Objects.requireNonNull(playerId, "playerId"), ignored -> new PlayerQuests());
    }

    public enum ActivationStatus { STARTED, ALREADY_ACTIVE, NO_SOUL, SOUL_ALREADY_USED, NOT_ENOUGH_LEVELS, PENDING_REVIEW }

    public enum ProgressStatus { ADVANCED, COMPLETED, EXPIRED }

    /** @param soulBoss the boss whose soul must be consumed when {@code status} is STARTED */
    public record Activation(ActivationStatus status, int cost, String soulBoss) {
        static Activation refused(ActivationStatus status) {
            return new Activation(status, 0, null);
        }
    }

    public record ActiveQuest(long startedAt, String soulBoss, double progress) {
        public ActiveQuest {
            Objects.requireNonNull(soulBoss, "soulBoss");
        }

        ActiveQuest withProgress(double updated) {
            return new ActiveQuest(startedAt, soulBoss, updated);
        }
    }

    public record Progress(ArmorQuestPiece piece, double previous, double current, int target,
                           ProgressStatus status) {
    }

    public record Expired(UUID playerId, ArmorQuestPiece piece) {
    }

    public record PendingSoul(String bossName, int amount) {
    }

    public record SoulDelivery(String bossId, int amount, String receiptId, String inventoryBefore, String inventoryAfter) {
        public SoulDelivery {
            if (bossId == null || bossId.isBlank() || amount < 1) throw new IllegalArgumentException("invalid soul receipt");
            UUID.fromString(receiptId);
            Objects.requireNonNull(inventoryBefore, "inventoryBefore");
        }
    }

    public record ArmorDelivery(ArmorQuestPiece piece, String receiptId, String inventoryBefore, String inventoryAfter) {
        public ArmorDelivery {
            Objects.requireNonNull(piece, "piece");
            UUID.fromString(receiptId);
            Objects.requireNonNull(inventoryBefore, "inventoryBefore");
        }
    }

    public record ActivationReceipt(ArmorQuestPiece piece, String soulBoss, int cost, String receiptId,
                                    String inventoryBefore, int levelBefore, float expBefore, int totalBefore,
                                    String inventoryAfter, int levelAfter, float expAfter, int totalAfter) {
        public ActivationReceipt {
            Objects.requireNonNull(piece, "piece");
            Objects.requireNonNull(soulBoss, "soulBoss");
            Objects.requireNonNull(inventoryBefore, "inventoryBefore");
            if (cost < 0) throw new IllegalArgumentException("negative cost");
            UUID.fromString(receiptId);
        }
    }

    /**
     * @param souls       boss whose soul each completed piece used
     * @param completions how many times each piece was completed
     */
    public record PlayerView(int activations, Map<ArmorQuestPiece, ActiveQuest> active,
                             Map<ArmorQuestPiece, String> souls, Map<ArmorQuestPiece, Integer> completions,
                             Map<String, PendingSoul> pendingSouls, Map<ArmorQuestPiece, Integer> pendingArmor,
                             ArmorDelivery armorDelivery, ActivationReceipt activationReceipt, SoulDelivery soulDelivery) {
        public PlayerView(int activations, Map<ArmorQuestPiece, ActiveQuest> active,
                          Map<ArmorQuestPiece, String> souls, Map<ArmorQuestPiece, Integer> completions,
                          Map<String, PendingSoul> pendingSouls) {
            this(activations, active, souls, completions, pendingSouls, Map.of(), null, null, null);
        }
    }

    private static final class PlayerQuests {
        private int activations;
        private final EnumMap<ArmorQuestPiece, ActiveQuest> active = new EnumMap<>(ArmorQuestPiece.class);
        private final EnumMap<ArmorQuestPiece, String> souls = new EnumMap<>(ArmorQuestPiece.class);
        private final EnumMap<ArmorQuestPiece, Integer> completions = new EnumMap<>(ArmorQuestPiece.class);
        private final Map<String, PendingSoul> pendingSouls = new LinkedHashMap<>();
        private final EnumMap<ArmorQuestPiece, Integer> pendingArmor = new EnumMap<>(ArmorQuestPiece.class);
        private ArmorDelivery armorDelivery;
        private ActivationReceipt activationReceipt;
        private SoulDelivery soulDelivery;
    }
}
