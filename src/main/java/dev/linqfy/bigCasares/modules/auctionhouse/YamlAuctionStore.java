package dev.linqfy.bigCasares.modules.auctionhouse;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import dev.linqfy.bigCasares.module.runtime.PlayerDataCheckpoint;

/** Stores listings and claim boxes in {@code data/auction-house.yml}, written atomically. */
final class YamlAuctionStore implements AuctionStore<ItemStack> {

    private final Path file;
    private final Logger logger;

    YamlAuctionStore(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    @Override
    public AuctionSnapshot<ItemStack> load() {
        if (!Files.exists(file)) {
            return AuctionSnapshot.empty();
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file.toFile());
        } catch (IOException | InvalidConfigurationException failure) {
            // Refusing to start keeps the unreadable file (and the items inside it) from being overwritten.
            throw new IllegalStateException("Could not read " + file, failure);
        }
        List<AuctionListing<ItemStack>> listings = new ArrayList<>();
        ConfigurationSection listingSection = yaml.getConfigurationSection("listings");
        if (listingSection != null) {
            for (String key : listingSection.getKeys(false)) {
                ConfigurationSection entry = listingSection.getConfigurationSection(key);
                try {
                    listings.add(readListing(key, entry));
                } catch (RuntimeException invalid) {
                    logger.warning("[AuctionHouse] Skipping invalid listing " + key + ": " + invalid.getMessage());
                }
            }
        }
        Map<UUID, List<ItemStack>> claims = new LinkedHashMap<>();
        ConfigurationSection claimSection = yaml.getConfigurationSection("claims");
        if (claimSection != null) {
            for (String key : claimSection.getKeys(false)) {
                try {
                    List<ItemStack> items = new ArrayList<>();
                    for (Object value : claimSection.getList(key, List.of())) {
                        if (value instanceof ItemStack item) {
                            items.add(item);
                        }
                    }
                    claims.put(UUID.fromString(key), items);
                } catch (IllegalArgumentException invalid) {
                    logger.warning("[AuctionHouse] Skipping invalid claim box " + key);
                }
            }
        }
        Map<UUID, AuctionClaim<ItemStack>> pendingClaims = new LinkedHashMap<>();
        ConfigurationSection pendingClaimSection = yaml.getConfigurationSection("pending-claims");
        if (yaml.contains("pending-claims") && pendingClaimSection == null) {
            throw new IllegalStateException("Invalid pending claims in " + file);
        }
        if (pendingClaimSection != null) {
            for (String key : pendingClaimSection.getKeys(false)) {
                try {
                    ConfigurationSection entry = pendingClaimSection.getConfigurationSection(key);
                    if (entry == null) throw new IllegalArgumentException("not a section");
                    pendingClaims.put(UUID.fromString(key), new AuctionClaim<>(
                        itemList(entry, "items"), entry.getString("inventory-before", ""),
                        entry.getString("inventory-after"), itemList(entry, "leftovers"),
                        entry.getString("receipt-id", "")));
                } catch (RuntimeException invalid) {
                    throw new IllegalStateException("Invalid pending claim " + key + " in " + file, invalid);
                }
            }
        }
        Map<Long, AuctionListingHold> pendingListings = new LinkedHashMap<>();
        ConfigurationSection pendingListingSection = yaml.getConfigurationSection("pending-listings");
        if (yaml.contains("pending-listings") && pendingListingSection == null) {
            throw new IllegalStateException("Invalid pending listings in " + file);
        }
        if (pendingListingSection != null) {
            for (String key : pendingListingSection.getKeys(false)) {
                try {
                    ConfigurationSection entry = pendingListingSection.getConfigurationSection(key);
                    if (entry == null) throw new IllegalArgumentException("not a section");
                    pendingListings.put(Long.parseLong(key), new AuctionListingHold(
                        UUID.fromString(entry.getString("seller", "")), entry.getString("inventory-before", ""),
                        entry.getString("inventory-after"), entry.getString("receipt-id", "")));
                } catch (RuntimeException invalid) {
                    throw new IllegalStateException("Invalid pending listing " + key + " in " + file, invalid);
                }
            }
        }
        return new AuctionSnapshot<>(yaml.getLong("next-id", 1), listings, claims, readPending(yaml, file), pendingClaims, pendingListings);
    }

    private static List<ItemStack> itemList(ConfigurationSection entry, String key) {
        List<ItemStack> items = new ArrayList<>();
        for (Object value : entry.getList(key, List.of())) {
            if (!(value instanceof ItemStack item)) throw new IllegalArgumentException("invalid item");
            items.add(item);
        }
        return items;
    }

    static Map<Long, AuctionPurchase> readPending(YamlConfiguration yaml, Path file) {
        Map<Long, AuctionPurchase> pending = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("pending-purchases");
        if (yaml.contains("pending-purchases") && section == null) {
            throw new IllegalStateException("Invalid pending purchases in " + file);
        }
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    ConfigurationSection entry = section.getConfigurationSection(key);
                    if (entry == null) throw new IllegalArgumentException("not a section");
                    pending.put(Long.parseLong(key), new AuctionPurchase(
                        UUID.fromString(entry.getString("buyer", "")),
                        AuctionPurchase.Phase.valueOf(entry.getString("phase", ""))));
                } catch (RuntimeException invalid) {
                    throw new IllegalStateException("Invalid pending purchase " + key + " in " + file, invalid);
                }
            }
        }
        return pending;
    }

    static void writePending(YamlConfiguration yaml, Map<Long, AuctionPurchase> pending) {
        ConfigurationSection section = yaml.createSection("pending-purchases");
        pending.forEach((id, purchase) -> {
            ConfigurationSection entry = section.createSection(Long.toString(id));
            entry.set("buyer", purchase.buyer().toString());
            entry.set("phase", purchase.phase().name());
        });
    }

    @Override
    public void save(AuctionSnapshot<ItemStack> snapshot) {
        try {
            saveListing(snapshot);
        } catch (IllegalStateException failure) {
            logger.log(Level.SEVERE, "[AuctionHouse] Could not save " + file, failure);
        }
    }

    @Override
    public void saveListing(AuctionSnapshot<ItemStack> snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-id", snapshot.nextId());
        ConfigurationSection listingSection = yaml.createSection("listings");
        for (AuctionListing<ItemStack> listing : snapshot.listings()) {
            ConfigurationSection entry = listingSection.createSection(Long.toString(listing.id()));
            entry.set("seller", listing.seller().toString());
            entry.set("seller-name", listing.sellerName());
            entry.set("item", listing.item());
            entry.set("price", listing.price());
            entry.set("created-at", listing.createdAt().toEpochMilli());
            entry.set("expires-at", listing.expiresAt().toEpochMilli());
        }
        ConfigurationSection claimSection = yaml.createSection("claims");
        snapshot.claims().forEach((player, items) -> claimSection.set(player.toString(), items));
        writePending(yaml, snapshot.pendingPurchases());
        ConfigurationSection pendingClaimSection = yaml.createSection("pending-claims");
        snapshot.pendingClaims().forEach((player, receipt) -> {
            ConfigurationSection entry = pendingClaimSection.createSection(player.toString());
            entry.set("items", receipt.items());
            entry.set("inventory-before", receipt.inventoryBefore());
            entry.set("inventory-after", receipt.inventoryAfter());
            entry.set("leftovers", receipt.leftovers());
            entry.set("receipt-id", receipt.receiptId());
        });
        ConfigurationSection pendingListingSection = yaml.createSection("pending-listings");
        snapshot.pendingListings().forEach((id, hold) -> {
            ConfigurationSection entry = pendingListingSection.createSection(Long.toString(id));
            entry.set("seller", hold.seller().toString());
            entry.set("inventory-before", hold.inventoryBefore());
            entry.set("inventory-after", hold.inventoryAfter());
            entry.set("receipt-id", hold.receiptId());
        });
        try {
            boolean newDirectory = !Files.isDirectory(file.getParent());
            Files.createDirectories(file.getParent());
            if (newDirectory && file.getParent().getParent() != null) {
                PlayerDataCheckpoint.forceDirectory(file.getParent().getParent());
            }
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, yaml.saveToString());
            PlayerDataCheckpoint.forceFile(temporary);
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            PlayerDataCheckpoint.forceDirectory(file.getParent());
        } catch (IOException failure) {
            throw new IllegalStateException("Could not save " + file, failure);
        }
    }

    private static AuctionListing<ItemStack> readListing(String key, ConfigurationSection entry) {
        if (entry == null) {
            throw new IllegalArgumentException("not a section");
        }
        ItemStack item = entry.getItemStack("item");
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("missing item");
        }
        return new AuctionListing<>(
            Long.parseLong(key),
            UUID.fromString(entry.getString("seller", "")),
            entry.getString("seller-name"),
            item,
            entry.getLong("price"),
            Instant.ofEpochMilli(entry.getLong("created-at")),
            Instant.ofEpochMilli(entry.getLong("expires-at"))
        );
    }
}
