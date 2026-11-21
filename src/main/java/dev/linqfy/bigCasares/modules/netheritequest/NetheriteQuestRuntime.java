package dev.linqfy.bigCasares.modules.netheritequest;

import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.Activation;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.ActiveQuest;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.PendingSoul;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.PlayerView;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.Progress;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.ProgressStatus;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import dev.linqfy.bigCasares.module.runtime.PlayerDataCheckpoint;
import dev.linqfy.bigCasares.module.runtime.InventoryCustody;

/** Applies the quest rules to live players: inventories, XP levels, rewards and messages. */
final class NetheriteQuestRuntime {

    /** Blocks between walking progress notices. */
    private static final int WALK_NOTICE_BLOCKS = 500;

    private final Server server;
    private final Logger logger;
    private final NetheriteQuestBook book;
    private final NetheriteQuestStore store;
    private final BossSoulItem souls;
    private final LongSupplier clock;
    private final NamespacedKey armorReceiptKey;
    private final NamespacedKey activationReceiptKey;
    private final NamespacedKey soulReceiptKey;
    private final InventoryCustody custody;
    private final Set<UUID> deferredForCustody = new LinkedHashSet<>();
    private boolean dirty;
    private boolean paused;

    NetheriteQuestRuntime(Server server, Logger logger, NetheriteQuestBook book, NetheriteQuestStore store,
                          BossSoulItem souls, LongSupplier clock) {
        this(server, logger, book, store, souls, clock, null, null, null);
    }

    NetheriteQuestRuntime(Server server, Logger logger, NetheriteQuestBook book, NetheriteQuestStore store,
                          BossSoulItem souls, LongSupplier clock, NamespacedKey armorReceiptKey,
                          NamespacedKey activationReceiptKey, NamespacedKey soulReceiptKey) {
        this.server = server;
        this.logger = logger;
        this.book = book;
        this.store = store;
        this.souls = souls;
        this.clock = clock;
        this.armorReceiptKey = armorReceiptKey;
        this.activationReceiptKey = activationReceiptKey;
        this.soulReceiptKey = soulReceiptKey;
        this.custody = book::hasInventoryReceipt;
    }

    NetheriteQuestBook book() {
        return book;
    }

    BossSoulItem souls() {
        return souls;
    }

    // ---------------------------------------------------------------- activation

    void activate(Player player, ArmorQuestPiece piece) {
        if (paused) return;
        UUID playerId = player.getUniqueId();
        if (ownedElsewhere(playerId)) {
            player.sendMessage("§cTu inventario está ocupado; intentá activar la misión más tarde.");
            return;
        }
        if (book.activationPending(playerId)) {
            recoverActivation(player);
            if (paused || book.activationPending(playerId)) {
                player.sendMessage("§cTu activación anterior requiere revisión. Contactá a un administrador.");
                return;
            }
        }
        List<String> held = heldSoulBosses(player.getInventory());
        int levelBefore = player.getLevel();
        Activation activation = book.activate(playerId, piece, held, levelBefore, clock.getAsLong());
        switch (activation.status()) {
            case PENDING_REVIEW -> player.sendMessage("§cTu activación anterior requiere revisión.");
            case ALREADY_ACTIVE -> player.sendMessage("§cYa tenés activa la misión del " + piece.displayName() + ".");
            case NO_SOUL -> player.sendMessage("§cNecesitás un §5Alma de Boss §cpara activar esta misión. "
                + "Se consigue quedando en el top 3 de daño de un boss.");
            case SOUL_ALREADY_USED -> player.sendMessage("§cTus almas son de bosses que ya usaste para otra pieza. "
                + "Cada pieza necesita el alma de un boss distinto.");
            case NOT_ENOUGH_LEVELS -> player.sendMessage("§cActivar esta misión cuesta §e" + activation.cost()
                + " niveles§c y tenés §e" + player.getLevel() + "§c.");
            case STARTED -> {
                try {
                    book.beginActivationReceipt(playerId, piece, activation.soulBoss(), activation.cost(),
                        inventorySnapshot(player), levelBefore, player.getExp(), player.getTotalExperience());
                } catch (RuntimeException failure) {
                    if (!book.activationPending(playerId)) book.cancelActivation(playerId, piece);
                    paused = true;
                    logger.log(Level.SEVERE, "[Netherita] No se pudo preparar la activación de " + playerId, failure);
                    player.sendMessage("§cLa activación quedó pausada. Contactá a un administrador.");
                    return;
                }
                var receipt = book.activationReceipt(playerId).orElseThrow();
                if (!saveChecked()) {
                    player.sendMessage("§cNo se pudo guardar la activación. Contactá a un administrador.");
                    return;
                }
                try {
                    if (!removeOneSoul(player.getInventory(), activation.soulBoss())) {
                        book.cancelUnpaidActivation(playerId, receipt.receiptId());
                        saveChecked();
                        player.sendMessage("§cNo se encontró el alma. Probá de nuevo.");
                        return;
                    }
                    if (activation.cost() > 0) player.giveExpLevels(-activation.cost());
                    book.recordActivationOutcome(playerId, inventorySnapshot(player), player.getLevel(), player.getExp(),
                        player.getTotalExperience());
                    if (!saveChecked()) return;
                    PlayerDataCheckpoint.save(player, activationReceiptKey, receipt.receiptId());
                    if (!book.finishActivation(playerId, receipt.receiptId())) throw new IllegalStateException("Activation receipt changed");
                    if (!saveChecked()) return;
                } catch (IOException | RuntimeException failure) {
                    logger.log(Level.SEVERE, "[Netherita] La activación requiere revisión para " + playerId, failure);
                    player.sendMessage("§cLa activación quedó pendiente de revisión. Contactá a un administrador.");
                    return;
                }
                player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 0.8f);
                player.sendMessage("§6§lMisión activada: §e" + piece.displayName());
                player.sendMessage("§7Objetivo: §f" + objective(piece) + "§7. Tenés §f"
                    + book.settings().durationHours() + " horas§7.");
                player.sendMessage("§7Costo: §e" + (activation.cost() == 0 ? "gratis" : activation.cost() + " niveles")
                    + "§7. La próxima cuesta §e" + book.nextActivationCost(playerId) + " niveles§7.");
            }
        }
    }

    // ---------------------------------------------------------------- progress

    void recordKill(Player killer, String entityKey) {
        if (paused) return;
        UUID playerId = killer.getUniqueId();
        for (ArmorQuestPiece piece : ArmorQuestPiece.values()) {
            if (paused) return;
            if (piece.killTarget().map(entityKey::equals).orElse(false)) {
                book.addProgress(playerId, piece, 1.0, clock.getAsLong())
                    .ifPresent(progress -> announce(killer, progress, true));
            }
        }
    }

    boolean walking(UUID playerId) {
        return !paused && book.hasActive(playerId, ArmorQuestPiece.BOOTS);
    }

    void recordWalk(Player player, double blocks) {
        if (paused) return;
        book.addProgress(player.getUniqueId(), ArmorQuestPiece.BOOTS, blocks, clock.getAsLong())
            .ifPresent(progress -> announce(player, progress,
                (int) (progress.current() / WALK_NOTICE_BLOCKS) > (int) (progress.previous() / WALK_NOTICE_BLOCKS)));
    }

    private void announce(Player player, Progress progress, boolean notice) {
        dirty = true;
        ArmorQuestPiece piece = progress.piece();
        switch (progress.status()) {
            case EXPIRED -> player.sendMessage("§cSe venció el tiempo de la misión del " + piece.displayName() + ".");
            case COMPLETED -> complete(player, piece);
            case ADVANCED -> {
                if (notice) {
                    player.sendActionBar("§6" + piece.displayName() + ": §e" + (long) progress.current()
                        + "§7/§e" + progress.target() + " §7" + piece.objectiveName());
                }
            }
        }
    }

    private void complete(Player player, ArmorQuestPiece piece) {
        if (!saveChecked()) {
            player.sendMessage("§cLa recompensa quedó pendiente de revisión por un error de guardado.");
            return;
        }
        deliverArmor(player);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        player.sendMessage("§6§l¡Misión completada! §eRecompensa: " + piece.displayName() + "§e.");
        server.broadcastMessage("§6" + player.getName() + " §7consiguió el §e" + piece.displayName() + "§7.");
    }

    void expireQuests() {
        if (paused) return;
        long now = clock.getAsLong();
        for (NetheriteQuestBook.Expired expired : book.expireAll(now)) {
            notifyExpired(expired.playerId(), expired.piece());
        }
    }

    private void notifyExpired(UUID playerId, ArmorQuestPiece piece) {
        dirty = true;
        Player player = server.getPlayer(playerId);
        if (player != null) {
            player.sendMessage("§cSe venció el tiempo de la misión del " + piece.displayName() + ".");
        }
    }

    // ---------------------------------------------------------------- souls

    /** Gives one soul of {@code bossId} to each of the top damage dealers, or keeps it until they log in. */
    void awardBossSouls(String bossId, String bossName, List<UUID> topDamagers) {
        if (paused) return;
        Set<UUID> winners = new LinkedHashSet<>(topDamagers);
        int placement = 0;
        for (UUID playerId : winners) {
            if (++placement > 3) {
                break;
            }
            book.addPendingSoul(playerId, bossId, bossName, 1);
            dirty = true;
        }
        if (!saveChecked()) return;
        winners.stream().limit(3).forEach(playerId -> {
            Player player = server.getPlayer(playerId);
            if (player != null && player.isOnline()) deliverSouls(player);
        });
    }

    void deliverPending(Player player) {
        if (paused) return;
        if (deferIfOwned(player.getUniqueId())) return;
        recoverActivation(player);
        if (paused) return;
        deliverArmor(player);
        if (paused) return;
        deliverSouls(player);
    }

    void claimRewards(Player player) {
        if (deferIfOwned(player.getUniqueId())) {
            player.sendMessage("§eTus recompensas siguen pendientes hasta que se libere tu inventario.");
            return;
        }
        recoverActivation(player);
        deliverArmor(player);
        if (!paused) deliverSouls(player);
    }

    private void deliverSouls(Player player) {
        if (paused || soulReceiptKey == null) return;
        UUID id = player.getUniqueId();
        if (deferIfOwned(id)) return;
        var existing = book.soulDelivery(id);
        if (existing.isPresent()) {
            try {
                var receipt = existing.get();
                if (receipt.inventoryAfter() == null || !PlayerDataCheckpoint.contains(player, soulReceiptKey, receipt.receiptId())) {
                    player.sendMessage("§cTu entrega de alma requiere revisión. Contactá a un administrador.");
                    return;
                }
                PlayerDataCheckpoint.confirm(player, soulReceiptKey, receipt.receiptId());
                book.resolveSoulDelivery(id, receipt.receiptId(), true);
                if (!saveChecked()) return;
            } catch (IOException | RuntimeException failure) {
                logger.log(Level.SEVERE, "[Netherita] No se pudo verificar la entrega de alma de " + id, failure);
                player.sendMessage("§cTu entrega de alma requiere revisión.");
                return;
            }
        }
        for (Map.Entry<String, PendingSoul> entry : book.view(id).pendingSouls().entrySet()) {
            int remaining = entry.getValue().amount();
            while (remaining > 0) {
                if (player.getInventory().firstEmpty() < 0) {
                    player.sendMessage("§eTu inventario está lleno. Usá /netherita reclamar cuando tengas espacio.");
                    return;
                }
                int amount = Math.min(64, remaining);
                ItemStack reward = souls.create(entry.getKey(), entry.getValue().bossName(), amount);
                String receipt = book.beginSoulDelivery(id, entry.getKey(), amount, inventorySnapshot(player)).orElseThrow();
                if (!saveChecked()) return;
                try {
                    if (!player.getInventory().addItem(reward).isEmpty()) throw new IllegalStateException("Soul delivery did not fit");
                    book.recordSoulOutcome(id, inventorySnapshot(player));
                    if (!saveChecked()) return;
                    PlayerDataCheckpoint.save(player, soulReceiptKey, receipt);
                    book.resolveSoulDelivery(id, receipt, true);
                    if (!saveChecked()) return;
                    player.sendMessage("§5Recibiste §l" + amount + "x Alma de " + entry.getValue().bossName() + "§5.");
                } catch (IOException | RuntimeException failure) {
                    logger.log(Level.SEVERE, "[Netherita] La entrega de alma requiere revisión para " + id, failure);
                    player.sendMessage("§cTu entrega de alma requiere revisión. Contactá a un administrador.");
                    return;
                }
                remaining -= amount;
            }
        }
    }

    private void recoverActivation(Player player) {
        UUID id = player.getUniqueId();
        if (deferIfOwned(id)) return;
        book.activationReceipt(id).ifPresent(receipt -> {
            try {
                if (receipt.inventoryAfter() == null || activationReceiptKey == null
                    || !PlayerDataCheckpoint.contains(player, activationReceiptKey, receipt.receiptId())) {
                    player.sendMessage("§cTu activación requiere revisión. Contactá a un administrador.");
                    return;
                }
                PlayerDataCheckpoint.confirm(player, activationReceiptKey, receipt.receiptId());
                if (!book.finishActivation(id, receipt.receiptId())) throw new IllegalStateException("Activation receipt changed");
                if (saveChecked()) player.sendMessage("§aSe confirmó tu activación pendiente de netherita.");
            } catch (IOException | RuntimeException failure) {
                logger.log(Level.SEVERE, "[Netherita] No se pudo verificar la activación de " + id, failure);
                player.sendMessage("§cTu activación requiere revisión. Contactá a un administrador.");
            }
        });
    }

    private static String inventorySnapshot(Player player) {
        ItemStack[] items = player.getInventory().getContents().clone();
        for (int slot = 0; slot < items.length; slot++) {
            if (items[slot] == null) items[slot] = ItemStack.empty();
        }
        return Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(items));
    }

    void deliverArmor(Player player) {
        if (paused || armorReceiptKey == null) return;
        UUID id = player.getUniqueId();
        if (deferIfOwned(id)) return;
        var existing = book.armorDelivery(id);
        if (existing.isPresent()) {
            try {
                var receipt = existing.get();
                if (receipt.inventoryAfter() == null || !PlayerDataCheckpoint.contains(player, armorReceiptKey, receipt.receiptId())) {
                    player.sendMessage("§cTu recompensa de netherita está pendiente de revisión. Contactá a un administrador.");
                    return;
                }
                PlayerDataCheckpoint.confirm(player, armorReceiptKey, receipt.receiptId());
                book.finishArmorDelivery(id, receipt.receiptId());
                if (!saveChecked()) return;
            } catch (IOException | RuntimeException failure) {
                logger.log(Level.SEVERE, "[Netherita] No se pudo verificar el recibo de armadura de " + id, failure);
                player.sendMessage("§cTu recompensa de netherita está pendiente de revisión.");
                return;
            }
        }
        for (ArmorQuestPiece piece : ArmorQuestPiece.values()) {
            while (book.pendingArmor(id).getOrDefault(piece, 0) > 0) {
                if (player.getInventory().firstEmpty() < 0) {
                    player.sendMessage("§eTu inventario está lleno. Usá /netherita reclamar cuando tengas espacio.");
                    return;
                }
                ItemStack reward = armor(piece);
                var receipt = book.beginArmorDelivery(id, piece, inventorySnapshot(player)).orElseThrow();
                if (!saveChecked()) return;
                try {
                    if (!player.getInventory().addItem(reward).isEmpty()) {
                        throw new IllegalStateException("Armor delivery did not fit");
                    }
                    book.recordArmorOutcome(id, inventorySnapshot(player));
                    if (!saveChecked()) return;
                    PlayerDataCheckpoint.save(player, armorReceiptKey, receipt.receiptId());
                    book.finishArmorDelivery(id, receipt.receiptId());
                    if (!saveChecked()) return;
                    player.sendMessage("§aRecibiste: " + piece.displayName() + ".");
                } catch (IOException | RuntimeException failure) {
                    logger.log(Level.SEVERE, "[Netherita] La entrega de armadura requiere revisión para " + id, failure);
                    player.sendMessage("§cTu recompensa de netherita está pendiente de revisión. Contactá a un administrador.");
                    return;
                }
            }
        }
    }

    List<String> heldSoulBosses(PlayerInventory inventory) {
        List<String> held = new ArrayList<>();
        for (ItemStack stack : inventory.getStorageContents()) {
            souls.bossOf(stack).filter(boss -> !held.contains(boss)).ifPresent(held::add);
        }
        return held;
    }

    private boolean removeOneSoul(PlayerInventory inventory, String bossId) {
        ItemStack[] contents = inventory.getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (souls.bossOf(stack).map(bossId::equals).orElse(false)) {
                if (stack.getAmount() <= 1) {
                    contents[slot] = null;
                } else {
                    stack.setAmount(stack.getAmount() - 1);
                }
                inventory.setStorageContents(contents);
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- status

    List<String> status(UUID playerId) {
        long now = clock.getAsLong();
        book.expireFor(playerId, now).forEach(piece -> notifyExpired(playerId, piece));
        PlayerView view = book.view(playerId);
        List<String> lines = new ArrayList<>();
        lines.add("§6§lMisiones de Netherita");
        for (ArmorQuestPiece piece : ArmorQuestPiece.values()) {
            ActiveQuest active = view.active().get(piece);
            String line;
            if (active != null) {
                line = "§e" + piece.displayName() + "§7: §f" + (long) active.progress() + "/"
                    + book.settings().target(piece) + " " + piece.objectiveName() + " §7(quedan §f"
                    + duration(book.remainingMillis(active, now)) + "§7)";
            } else if (view.souls().containsKey(piece)) {
                line = "§a" + piece.displayName() + "§7: completada §8(alma: " + view.souls().get(piece) + ")";
            } else {
                line = "§7" + piece.displayName() + ": §f" + objective(piece);
            }
            lines.add(line);
        }
        lines.add("§7Próxima activación: §e" + book.nextActivationCost(playerId) + " niveles§7 + 1 Alma de Boss.");
        lines.add("§7Usá §f/netherita activar <casco|peto|pantalones|botas>§7.");
        return lines;
    }

    String objective(ArmorQuestPiece piece) {
        int target = book.settings().target(piece);
        return piece.killTarget().isPresent()
            ? "matar " + target + " " + piece.objectiveName()
            : "caminar " + target + " bloques";
    }

    private static String duration(long millis) {
        long minutes = millis / 60_000L;
        return String.format(Locale.ROOT, "%dh %02dm", minutes / 60L, minutes % 60L);
    }

    // ---------------------------------------------------------------- items

    static ItemStack armor(ArmorQuestPiece piece) {
        Material material = switch (piece) {
            case HELMET -> Material.NETHERITE_HELMET;
            case CHESTPLATE -> Material.NETHERITE_CHESTPLATE;
            case LEGGINGS -> Material.NETHERITE_LEGGINGS;
            case BOOTS -> Material.NETHERITE_BOOTS;
        };
        Enchantment enchantment = switch (piece) {
            case HELMET -> Enchantment.RESPIRATION;
            case CHESTPLATE -> Enchantment.BLAST_PROTECTION;
            case LEGGINGS -> Enchantment.SWIFT_SNEAK;
            case BOOTS -> Enchantment.UNBREAKING;
        };
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.addEnchant(enchantment, piece.enchantmentLevel(), true);
        meta.setLore(List.of("§7Ganada en una misión de netherita."));
        stack.setItemMeta(meta);
        return stack;
    }

    boolean queueAdminSoul(Player player, String bossId, int amount) {
        if (paused) return false;
        book.addPendingSoul(player.getUniqueId(), bossId, bossId, amount);
        dirty = true;
        if (!saveChecked()) return false;
        deliverSouls(player);
        return true;
    }

    InventoryCustody custodyProvider() { return custody; }

    private boolean ownedElsewhere(UUID playerId) {
        return InventoryCustody.ownedByOther(server, playerId, custody);
    }

    private boolean deferIfOwned(UUID playerId) {
        if (!ownedElsewhere(playerId)) return false;
        deferredForCustody.add(playerId);
        return true;
    }

    void retryDeferred() {
        if (paused) return;
        for (UUID playerId : List.copyOf(deferredForCustody)) {
            Player player = server.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                deferredForCustody.remove(playerId);
            } else if (!ownedElsewhere(playerId)) {
                deferredForCustody.remove(playerId);
                deliverPending(player);
            }
        }
    }

    // ---------------------------------------------------------------- storage

    void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    void save() {
        if (paused) return;
        saveChecked();
    }

    private boolean saveChecked() {
        try {
            store.save(book);
            dirty = false;
            return true;
        } catch (IOException | RuntimeException failure) {
            paused = true;
            logger.log(Level.SEVERE, "[Netherita] No se pudieron guardar las misiones.", failure);
            return false;
        }
    }

    boolean paused() { return paused; }

    boolean resolveReceipt(UUID playerId, String receipt, String kind, boolean confirmed) {
        if (paused || ownedElsewhere(playerId)) return false;
        boolean resolved = switch (kind) {
            case "activacion" -> book.resolveActivation(playerId, receipt, confirmed);
            case "armadura" -> book.resolveArmorDelivery(playerId, receipt, confirmed);
            case "alma" -> book.resolveSoulDelivery(playerId, receipt, confirmed);
            default -> false;
        };
        return resolved && saveChecked();
    }

    void markDirty() {
        dirty = true;
    }
}
