package dev.linqfy.bigCasares.modules.netheritequest;

import dev.linqfy.bigCasares.module.runtime.PlayerDataCheckpoint;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.ActiveQuest;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.PendingSoul;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.PlayerView;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.ArmorDelivery;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.ActivationReceipt;
import dev.linqfy.bigCasares.modules.netheritequest.NetheriteQuestBook.SoulDelivery;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Keeps quest progress, used souls and souls owed to offline players in one YAML file. */
public final class NetheriteQuestStore {

    private final Path file;

    public NetheriteQuestStore(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    public void loadInto(NetheriteQuestBook book) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(Files.readString(file));
        } catch (InvalidConfigurationException invalid) {
            throw new IOException("Archivo de misiones de netherita inválido: " + file, invalid);
        }
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ConfigurationSection section = players.getConfigurationSection(key);
            if (section != null) {
                try {
                    book.restore(playerId, read(section));
                } catch (RuntimeException invalid) {
                    throw new IOException("Invalid netherite quest receipt for " + playerId, invalid);
                }
            }
        }
    }

    public void save(NetheriteQuestBook book) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        book.snapshot().forEach((playerId, view) -> write(yaml.createSection("players." + playerId), view));
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, yaml.saveToString());
        PlayerDataCheckpoint.forceFile(temporary);
        Files.move(temporary, file, java.nio.file.StandardCopyOption.ATOMIC_MOVE,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        if (parent != null) {
            PlayerDataCheckpoint.forceDirectory(parent);
        }
    }

    private static PlayerView read(ConfigurationSection section) {
        EnumMap<ArmorQuestPiece, ActiveQuest> active = new EnumMap<>(ArmorQuestPiece.class);
        ConfigurationSection activeSection = section.getConfigurationSection("active");
        if (activeSection != null) {
            for (String key : activeSection.getKeys(false)) {
                ConfigurationSection quest = activeSection.getConfigurationSection(key);
                String soul = quest == null ? null : quest.getString("soul");
                ArmorQuestPiece.parse(key).filter(ignored -> soul != null).ifPresent(piece -> active.put(piece,
                    new ActiveQuest(quest.getLong("started"), soul, Math.max(0.0, quest.getDouble("progress")))));
            }
        }
        EnumMap<ArmorQuestPiece, String> souls = new EnumMap<>(ArmorQuestPiece.class);
        ConfigurationSection soulSection = section.getConfigurationSection("souls");
        if (soulSection != null) {
            for (String key : soulSection.getKeys(false)) {
                String boss = soulSection.getString(key);
                ArmorQuestPiece.parse(key).filter(ignored -> boss != null).ifPresent(piece -> souls.put(piece, boss));
            }
        }
        EnumMap<ArmorQuestPiece, Integer> completions = new EnumMap<>(ArmorQuestPiece.class);
        ConfigurationSection completionSection = section.getConfigurationSection("completions");
        if (completionSection != null) {
            for (String key : completionSection.getKeys(false)) {
                int count = completionSection.getInt(key);
                ArmorQuestPiece.parse(key).filter(ignored -> count > 0)
                    .ifPresent(piece -> completions.put(piece, count));
            }
        }
        Map<String, PendingSoul> pending = new LinkedHashMap<>();
        ConfigurationSection pendingSection = section.getConfigurationSection("pending-souls");
        if (pendingSection != null) {
            for (String boss : pendingSection.getKeys(false)) {
                int amount = pendingSection.getInt(boss + ".amount");
                if (amount > 0) {
                    pending.put(boss, new PendingSoul(pendingSection.getString(boss + ".name", boss), amount));
                }
            }
        }
        EnumMap<ArmorQuestPiece, Integer> pendingArmor = new EnumMap<>(ArmorQuestPiece.class);
        ConfigurationSection armorSection = section.getConfigurationSection("pending-armor");
        if (section.contains("pending-armor") && armorSection == null) throw new IllegalArgumentException("invalid armor rewards");
        if (armorSection != null) {
            for (String key : armorSection.getKeys(false)) {
                ArmorQuestPiece piece = ArmorQuestPiece.parse(key).orElseThrow(() -> new IllegalArgumentException("invalid armor piece"));
                int count = armorSection.getInt(key);
                if (count < 1) throw new IllegalArgumentException("invalid pending armor count");
                pendingArmor.put(piece, count);
            }
        }
        ArmorDelivery delivery = null;
        ConfigurationSection deliverySection = section.getConfigurationSection("armor-delivery");
        if (section.contains("armor-delivery") && deliverySection == null) throw new IllegalArgumentException("invalid armor receipt");
        if (deliverySection != null) {
            ArmorQuestPiece piece = ArmorQuestPiece.parse(deliverySection.getString("piece", ""))
                .orElseThrow(() -> new IllegalArgumentException("invalid armor piece"));
            delivery = new ArmorDelivery(piece, deliverySection.getString("receipt", ""),
                deliverySection.getString("inventory-before", ""), deliverySection.getString("inventory-after"));
            if (pendingArmor.getOrDefault(piece, 0) < 1) throw new IllegalArgumentException("receipt without reward");
        }
        ActivationReceipt activationReceipt = null;
        ConfigurationSection activationSection = section.getConfigurationSection("activation-receipt");
        if (section.contains("activation-receipt") && activationSection == null) throw new IllegalArgumentException("invalid activation receipt");
        if (activationSection != null) {
            ArmorQuestPiece piece = ArmorQuestPiece.parse(activationSection.getString("piece", ""))
                .orElseThrow(() -> new IllegalArgumentException("invalid activation piece"));
            activationReceipt = new ActivationReceipt(piece, activationSection.getString("soul", ""),
                activationSection.getInt("cost", -1), activationSection.getString("receipt", ""),
                activationSection.getString("inventory-before", ""), activationSection.getInt("level-before"),
                (float) activationSection.getDouble("exp-before"), activationSection.getInt("total-before"),
                activationSection.getString("inventory-after"), activationSection.getInt("level-after"),
                (float) activationSection.getDouble("exp-after"), activationSection.getInt("total-after"));
            ActiveQuest quest = active.get(piece);
            if (quest == null || !quest.soulBoss().equals(activationReceipt.soulBoss())) {
                throw new IllegalArgumentException("activation receipt without matching quest");
            }
        }
        SoulDelivery soulDelivery = null;
        ConfigurationSection soulDeliverySection = section.getConfigurationSection("soul-delivery");
        if (section.contains("soul-delivery") && soulDeliverySection == null) throw new IllegalArgumentException("invalid soul receipt");
        if (soulDeliverySection != null) {
            soulDelivery = new SoulDelivery(soulDeliverySection.getString("boss", ""), soulDeliverySection.getInt("amount"),
                soulDeliverySection.getString("receipt", ""), soulDeliverySection.getString("inventory-before", ""),
                soulDeliverySection.getString("inventory-after"));
            PendingSoul owed = pending.get(soulDelivery.bossId());
            if (owed == null || owed.amount() < soulDelivery.amount()) throw new IllegalArgumentException("soul receipt without reward");
        }
        return new PlayerView(section.getInt("activations"), active, souls, completions, pending, pendingArmor, delivery, activationReceipt, soulDelivery);
    }

    private static void write(ConfigurationSection section, PlayerView view) {
        section.set("activations", view.activations());
        view.active().forEach((piece, quest) -> {
            String path = "active." + piece.id();
            section.set(path + ".started", quest.startedAt());
            section.set(path + ".soul", quest.soulBoss());
            section.set(path + ".progress", quest.progress());
        });
        view.souls().forEach((piece, boss) -> section.set("souls." + piece.id(), boss));
        view.completions().forEach((piece, count) -> section.set("completions." + piece.id(), count));
        view.pendingSouls().forEach((boss, soul) -> {
            section.set("pending-souls." + boss + ".name", soul.bossName());
            section.set("pending-souls." + boss + ".amount", soul.amount());
        });
        view.pendingArmor().forEach((piece, count) -> section.set("pending-armor." + piece.id(), count));
        if (view.armorDelivery() != null) {
            section.set("armor-delivery.piece", view.armorDelivery().piece().id());
            section.set("armor-delivery.receipt", view.armorDelivery().receiptId());
            section.set("armor-delivery.inventory-before", view.armorDelivery().inventoryBefore());
            section.set("armor-delivery.inventory-after", view.armorDelivery().inventoryAfter());
        }
        if (view.activationReceipt() != null) {
            ActivationReceipt receipt = view.activationReceipt();
            section.set("activation-receipt.piece", receipt.piece().id());
            section.set("activation-receipt.soul", receipt.soulBoss());
            section.set("activation-receipt.cost", receipt.cost());
            section.set("activation-receipt.receipt", receipt.receiptId());
            section.set("activation-receipt.inventory-before", receipt.inventoryBefore());
            section.set("activation-receipt.level-before", receipt.levelBefore());
            section.set("activation-receipt.exp-before", (double) receipt.expBefore());
            section.set("activation-receipt.total-before", receipt.totalBefore());
            section.set("activation-receipt.inventory-after", receipt.inventoryAfter());
            section.set("activation-receipt.level-after", receipt.levelAfter());
            section.set("activation-receipt.exp-after", (double) receipt.expAfter());
            section.set("activation-receipt.total-after", receipt.totalAfter());
        }
        if (view.soulDelivery() != null) {
            section.set("soul-delivery.boss", view.soulDelivery().bossId());
            section.set("soul-delivery.amount", view.soulDelivery().amount());
            section.set("soul-delivery.receipt", view.soulDelivery().receiptId());
            section.set("soul-delivery.inventory-before", view.soulDelivery().inventoryBefore());
            section.set("soul-delivery.inventory-after", view.soulDelivery().inventoryAfter());
        }
    }
}
