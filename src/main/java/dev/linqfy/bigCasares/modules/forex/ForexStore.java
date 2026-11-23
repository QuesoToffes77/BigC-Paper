package dev.linqfy.bigCasares.modules.forex;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/** Saves the whole market to {@code forex/market.yml}. Writes go to a temporary file first. */
public final class ForexStore {

    private final File file;
    private final Logger logger;

    public ForexStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public void save(ForexMarket market) {
        save(capture(market));
    }

    void save(Snapshot snapshot) {
        try {
            write(serialize(snapshot));
        } catch (IOException failure) {
            throw new IllegalStateException("[Forex] Could not save the market", failure);
        }
    }

    /** Refuses to start on an unreadable file, so a later save never overwrites the players' money with nothing. */
    public void load(ForexMarket market) {
        if (!file.isFile()) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException failure) {
            throw new IllegalStateException("[Forex] " + file + " is unreadable; fix or move it before enabling", failure);
        }
        deserialize(yaml, market, logger);
    }

    record AccountSnapshot(UUID id, double emeralds, int rugPulls, long lastRugAt, boolean auraHidden) { }

    record CoinSnapshot(String ticker, String name, UUID creator, long createdAt, double creatorFee,
                        double stakingShare, double hype, double supply, long pool, long dividendPot,
                        String team, long nextFlipAt, int warningsSent, int flipsSurvived, long lastDividendAt,
                        double peakCap, long lifetimeVolume, List<Candle> candles, Candle current,
                        Map<UUID, Double> holdings, Map<UUID, Double> stakes, Map<UUID, Long> locks) {
        CoinSnapshot {
            candles = List.copyOf(candles);
            holdings = Map.copyOf(holdings);
            stakes = Map.copyOf(stakes);
            locks = Map.copyOf(locks);
        }
    }

    record Snapshot(long nextCycleAt, double emeraldPrice, double emeraldHigh, List<Candle> emeraldCandles,
                    List<AccountSnapshot> accounts, List<CoinSnapshot> coins, List<DeadCoin> graves) {
        Snapshot {
            emeraldCandles = List.copyOf(emeraldCandles);
            accounts = List.copyOf(accounts);
            coins = List.copyOf(coins);
            graves = List.copyOf(graves);
        }
    }

    static Snapshot capture(ForexMarket market) {
        EmeraldMarket emerald = market.emerald();
        List<AccountSnapshot> accounts = new ArrayList<>();
        for (Map.Entry<UUID, ForexMarket.Account> entry : market.accounts().entrySet()) {
            ForexMarket.Account account = entry.getValue();
            if (!account.isEmpty()) accounts.add(new AccountSnapshot(entry.getKey(), account.emeralds,
                account.rugPulls, account.lastRugAt, account.auraHidden));
        }
        List<CoinSnapshot> coins = new ArrayList<>();
        for (Shitcoin coin : market.coins()) {
            coins.add(new CoinSnapshot(coin.ticker(), coin.name(), coin.creator(), coin.createdAt(), coin.creatorFee(),
                coin.stakingShare(), coin.hype(), coin.supply(), coin.pool(), coin.dividendPot(), coin.teamId().orElse(null),
                coin.nextFlipAt(), coin.warningsSent(), coin.flipsSurvived(), coin.lastDividendAt(), coin.peakCap(),
                coin.lifetimeVolume(), coin.series().closedCandles(), coin.series().current(), coin.holdings(),
                coin.stakes(), coin.locks()));
        }
        return new Snapshot(market.nextCycleAt(), emerald.price(), emerald.allTimeHigh(), emerald.series().closedCandles(),
            accounts, coins, market.graveyard());
    }

    static YamlConfiguration serialize(ForexMarket market) {
        return serialize(capture(market));
    }

    static YamlConfiguration serialize(Snapshot snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-cycle-at", snapshot.nextCycleAt());
        yaml.set("emerald.price", snapshot.emeraldPrice());
        yaml.set("emerald.all-time-high", snapshot.emeraldHigh());
        yaml.set("emerald.candles", snapshot.emeraldCandles().stream().map(Candle::encode).toList());
        for (AccountSnapshot account : snapshot.accounts()) {
            String path = "accounts." + account.id();
            yaml.set(path + ".emeralds", account.emeralds());
            yaml.set(path + ".rug-pulls", account.rugPulls());
            yaml.set(path + ".last-rug-at", account.lastRugAt());
            yaml.set(path + ".aura-hidden", account.auraHidden());
        }
        for (CoinSnapshot coin : snapshot.coins()) {
            String path = "coins." + coin.ticker();
            yaml.set(path + ".name", coin.name());
            yaml.set(path + ".creator", coin.creator().toString());
            yaml.set(path + ".created-at", coin.createdAt());
            yaml.set(path + ".creator-fee", coin.creatorFee());
            yaml.set(path + ".staking-share", coin.stakingShare());
            yaml.set(path + ".hype", coin.hype());
            yaml.set(path + ".supply", coin.supply());
            yaml.set(path + ".pool", coin.pool());
            yaml.set(path + ".dividend-pot", coin.dividendPot());
            yaml.set(path + ".team", coin.team());
            yaml.set(path + ".next-flip-at", coin.nextFlipAt());
            yaml.set(path + ".warnings-sent", coin.warningsSent());
            yaml.set(path + ".flips-survived", coin.flipsSurvived());
            yaml.set(path + ".last-dividend-at", coin.lastDividendAt());
            yaml.set(path + ".peak-cap", coin.peakCap());
            yaml.set(path + ".lifetime-volume", coin.lifetimeVolume());
            yaml.set(path + ".candles", coin.candles().stream().map(Candle::encode).toList());
            yaml.set(path + ".candle", coin.current().encode());
            for (Map.Entry<UUID, Double> holding : coin.holdings().entrySet()) {
                String holder = path + ".holders." + holding.getKey();
                yaml.set(holder + ".balance", holding.getValue());
                double staked = coin.stakes().getOrDefault(holding.getKey(), 0.0);
                if (staked > Shitcoin.DUST) {
                    yaml.set(holder + ".staked", staked);
                    yaml.set(holder + ".locked-until", coin.locks().getOrDefault(holding.getKey(), 0L));
                }
            }
        }
        for (int index = 0; index < snapshot.graves().size(); index++) {
            DeadCoin dead = snapshot.graves().get(index);
            String path = "graveyard." + index;
            yaml.set(path + ".ticker", dead.ticker());
            yaml.set(path + ".name", dead.name());
            yaml.set(path + ".creator", dead.creator().toString());
            yaml.set(path + ".fate", dead.fate().name());
            yaml.set(path + ".peak-cap", dead.peakCap());
            yaml.set(path + ".lost-pool", dead.lostPool());
            yaml.set(path + ".died-at", dead.diedAt());
            yaml.set(path + ".candles", dead.candles().stream().map(Candle::encode).toList());
        }
        return yaml;
    }

    static void deserialize(ConfigurationSection yaml, ForexMarket market, Logger logger) {
        // Parse the entire snapshot before mutating the live market. Skipping a bad financial row
        // would let a subsequent save replace its only on-disk copy with an incomplete market.
        try {
            checkNumbers(yaml, "next-cycle-at", "emerald.price", "emerald.all-time-high");
            checkIntegers(yaml, Long.MAX_VALUE, "next-cycle-at");
            checkStringList(yaml, "emerald.candles");
            long nextCycle = yaml.getLong("next-cycle-at", 0);
            double emeraldPrice = yaml.getDouble("emerald.price", 0);
            double emeraldHigh = yaml.getDouble("emerald.all-time-high", 0);
            List<Candle> emeraldCandles = candles(yaml.getStringList("emerald.candles"));
            Map<UUID, ForexMarket.Account> parsedAccounts = new LinkedHashMap<>();
            ConfigurationSection accounts = checkedSection(yaml, "accounts");
            if (accounts != null) {
                for (String id : accounts.getKeys(false)) {
                    ConfigurationSection entry = requiredSection(accounts, id);
                    checkNumbers(entry, "emeralds", "rug-pulls", "last-rug-at");
                    checkIntegers(entry, Integer.MAX_VALUE, "rug-pulls");
                    checkIntegers(entry, Long.MAX_VALUE, "last-rug-at");
                    ForexMarket.Account account = new ForexMarket.Account();
                    account.emeralds = entry.getDouble("emeralds");
                    account.rugPulls = entry.getInt("rug-pulls");
                    account.lastRugAt = entry.getLong("last-rug-at");
                    if (entry.isSet("aura-hidden") && !entry.isBoolean("aura-hidden")) {
                        throw new IllegalArgumentException("Invalid aura-hidden");
                    }
                    account.auraHidden = entry.getBoolean("aura-hidden");
                    if (parsedAccounts.put(UUID.fromString(id), account) != null) {
                        throw new IllegalArgumentException("Duplicate account UUID");
                    }
                }
            }
            List<Shitcoin> parsedCoins = new ArrayList<>();
            ConfigurationSection coins = checkedSection(yaml, "coins");
            if (coins != null) {
                for (String ticker : coins.getKeys(false)) {
                    parsedCoins.add(readCoin(ticker, requiredSection(coins, ticker)));
                }
            }
            List<DeadCoin> parsedGraves = new ArrayList<>();
            ConfigurationSection graves = checkedSection(yaml, "graveyard");
            if (graves != null) {
                for (String key : graves.getKeys(false)) {
                    ConfigurationSection entry = requiredSection(graves, key);
                    checkNumbers(entry, "peak-cap", "lost-pool", "died-at");
                    checkIntegers(entry, Long.MAX_VALUE, "lost-pool", "died-at");
                    checkStringList(entry, "candles");
                    parsedGraves.add(new DeadCoin(entry.getString("ticker"), entry.getString("name"),
                        UUID.fromString(entry.getString("creator")), DeadCoin.Fate.valueOf(entry.getString("fate")),
                        entry.getDouble("peak-cap"), entry.getLong("lost-pool"), entry.getLong("died-at"),
                        candles(entry.getStringList("candles"))));
                }
            }
            market.restoreClock(nextCycle);
            market.emerald().restore(emeraldPrice, emeraldHigh, emeraldCandles);
            market.accounts().putAll(parsedAccounts);
            parsedCoins.forEach(market::restoreCoin);
            parsedGraves.forEach(market::restoreGrave);
        } catch (RuntimeException invalid) {
            throw new IllegalStateException("[Forex] Invalid market data; preserve the original file and repair it before enabling", invalid);
        }
    }

    private static ConfigurationSection checkedSection(ConfigurationSection parent, String key) {
        ConfigurationSection child = parent.getConfigurationSection(key);
        if (parent.isSet(key) && child == null) throw new IllegalArgumentException("Invalid section: " + key);
        return child;
    }

    private static ConfigurationSection requiredSection(ConfigurationSection parent, String key) {
        ConfigurationSection child = checkedSection(parent, key);
        if (child == null) throw new IllegalArgumentException("Missing section: " + key);
        return child;
    }

    private static void checkNumbers(ConfigurationSection section, String... keys) {
        for (String key : keys) {
            if (!section.isSet(key)) continue; // Older snapshots may omit optional values.
            Object raw = section.get(key);
            if (!(raw instanceof Number number) || !Double.isFinite(number.doubleValue()) || number.doubleValue() < 0) {
                throw new IllegalArgumentException("Invalid nonnegative number: " + key);
            }
        }
    }

    private static void checkIntegers(ConfigurationSection section, long maximum, String... keys) {
        for (String key : keys) {
            if (!section.isSet(key)) continue;
            Object raw = section.get(key);
            if (!(raw instanceof Number number)) throw new IllegalArgumentException("Invalid integer: " + key);
            BigDecimal value = new BigDecimal(number.toString());
            if (value.signum() < 0 || value.stripTrailingZeros().scale() > 0
                || value.compareTo(BigDecimal.valueOf(maximum)) > 0) {
                throw new IllegalArgumentException("Invalid integer: " + key);
            }
        }
    }

    private static void checkStringList(ConfigurationSection section, String key) {
        if (!section.isSet(key)) return;
        Object raw = section.get(key);
        if (!(raw instanceof List<?> list) || list.stream().anyMatch(item -> !(item instanceof String))) {
            throw new IllegalArgumentException("Invalid list: " + key);
        }
    }

    private static Shitcoin readCoin(String ticker, ConfigurationSection entry) {
        checkNumbers(entry, "created-at", "creator-fee", "staking-share", "hype", "supply", "pool",
            "dividend-pot", "next-flip-at", "warnings-sent", "flips-survived", "last-dividend-at",
            "peak-cap", "lifetime-volume");
        checkIntegers(entry, Long.MAX_VALUE, "created-at", "pool", "dividend-pot", "next-flip-at",
            "last-dividend-at", "lifetime-volume");
        checkIntegers(entry, Integer.MAX_VALUE, "warnings-sent", "flips-survived");
        if (entry.getDouble("creator-fee") > 1 || entry.getDouble("staking-share") > 1) {
            throw new IllegalArgumentException("Invalid fee or staking share");
        }
        checkStringList(entry, "candles");
        Shitcoin coin = new Shitcoin(ticker, entry.getString("name", ticker),
            UUID.fromString(entry.getString("creator")), entry.getLong("created-at"),
            entry.getDouble("creator-fee"), entry.getDouble("staking-share"));
        coin.hype(entry.getDouble("hype", 1.0));
        coin.supply(entry.getDouble("supply"));
        coin.pool(entry.getLong("pool"));
        coin.dividendPot(entry.getLong("dividend-pot"));
        coin.teamId(entry.getString("team"));
        coin.nextFlipAt(entry.getLong("next-flip-at"));
        coin.warningsSent(entry.getInt("warnings-sent"));
        coin.flipsSurvived(entry.getInt("flips-survived"));
        coin.lastDividendAt(entry.getLong("last-dividend-at", coin.createdAt()));
        coin.peakCap(entry.getDouble("peak-cap"));
        coin.lifetimeVolume(entry.getLong("lifetime-volume"));
        String current = entry.getString("candle");
        coin.series().restore(candles(entry.getStringList("candles")),
            current == null ? Candle.flat(0) : Candle.decode(current));
        ConfigurationSection holders = checkedSection(entry, "holders");
        if (holders != null) {
            for (String id : holders.getKeys(false)) {
                ConfigurationSection holder = requiredSection(holders, id);
                checkNumbers(holder, "balance", "staked", "locked-until");
                checkIntegers(holder, Long.MAX_VALUE, "locked-until");
                if (holder.getDouble("staked") > holder.getDouble("balance")) {
                    throw new IllegalArgumentException("Stake exceeds balance");
                }
                coin.restoreHolding(UUID.fromString(id), holder.getDouble("balance"), holder.getDouble("staked"),
                    holder.getLong("locked-until"));
            }
        }
        return coin;
    }

    private static List<Candle> candles(List<String> encoded) {
        return encoded.stream().map(Candle::decode).toList();
    }

    private void write(YamlConfiguration yaml) throws IOException {
        Path target = file.toPath();
        JournaledForexBank.ensureDirectory(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        byte[] bytes = yaml.saveToString().getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        JournaledForexBank.syncDirectory(target.getParent());
    }
}
