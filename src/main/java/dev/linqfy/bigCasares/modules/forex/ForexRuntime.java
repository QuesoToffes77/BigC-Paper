package dev.linqfy.bigCasares.modules.forex;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleUnaryOperator;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Glue between the market and the server: player-facing operations shared by the command and the menus,
 * the market tick, persistence and the whale aura.
 */
final class ForexRuntime {

    private final Plugin plugin;
    private final ForexMarket market;
    private final ForexBankBridge bankBridge;
    private final JournaledForexBank journaledBank;
    private final ForexStore store;
    private final ForexAnnouncer announcer;
    private final ForexTeams teams;
    private final ForexBoard board;
    private final ForexCharts charts;
    private ForexAsyncWriter writer;
    private List<Runnable> deferredEffects;
    private ForexCommittedViews committedViews;

    ForexRuntime(Plugin plugin, ForexSettings settings, ForexBank bank, ForexTeams teams) {
        this.plugin = plugin;
        LongSupplier clock = System::currentTimeMillis;
        this.bankBridge = new ForexBankBridge(bank, Bukkit::isPrimaryThread);
        this.journaledBank = new JournaledForexBank(bankBridge,
            new File(new File(plugin.getDataFolder(), "forex"), "transfers.log").toPath(),
            new File(new File(plugin.getDataFolder(), "forex"), "market.yml").toPath());
        this.market = new ForexMarket(settings, journaledBank, new Random(), clock);
        this.store = new ForexStore(new File(new File(plugin.getDataFolder(), "forex"), "market.yml"),
            plugin.getLogger());
        this.announcer = new ForexAnnouncer(plugin.getServer(), clock);
        this.teams = teams;
        this.board = new ForexBoard(this);
        this.charts = new ForexCharts(plugin.getServer(), new ChartFeed(market),
            new File(new File(plugin.getDataFolder(), "forex"), "charts.yml"), plugin.getLogger(),
            () -> !paused() && !busy());
    }

    ForexMarket market() {
        return market;
    }

    ForexTeams teams() {
        return teams;
    }

    ForexBoard board() {
        return board;
    }

    ForexAnnouncer announcer() {
        return announcer;
    }

    boolean start() {
        java.nio.file.Path marketPath = new File(new File(plugin.getDataFolder(), "forex"), "market.yml").toPath();
        if (ForexAsyncWriter.leased(marketPath)) {
            journaledBank.pause();
            plugin.getLogger().warning("[Forex] Previous market write is still finishing; reload after it completes.");
            return false;
        }
        if (journaledBank.paused()) {
            plugin.getLogger().severe("[Forex] Unresolved transfers.log: market disabled pending manual reconciliation. "
                + "If a previous write just finished and the journal is absent, reload the module.");
            return false;
        }
        ForexAsyncWriter owned;
        try {
            owned = new ForexAsyncWriter(marketPath);
        } catch (IllegalStateException stillWriting) {
            journaledBank.pause();
            plugin.getLogger().warning("[Forex] Previous market write is still finishing; reload after it completes.");
            return false;
        }
        try {
            store.load(market);
            charts.start();
            refreshCommittedViews();
            writer = owned;
            return true;
        } catch (RuntimeException failure) {
            owned.close();
            throw failure;
        }
    }

    void stop() {
        try {
            charts.stop();
        } finally {
            ForexAsyncWriter owned = writer;
            bankBridge.close();
            try {
                if (owned != null && !journaledBank.paused() && !owned.busy()) {
                    // A pending snapshot already contains the last mutation: no second write may race it.
                    try { saveAsync(() -> {}, failure -> {}); }
                    catch (RuntimeException failure) { journaledBank.pause(); }
                }
            } finally {
                if (owned != null) owned.close();
                writer = null;
                announcer.clear();
            }
        }
    }

    boolean busy() { return writer != null && writer.busy(); }

    void pollWriter() {
        bankBridge.pump();
        ForexAsyncWriter current = writer;
        if (current != null) current.poll();
    }

    private void saveAsync(Runnable success, java.util.function.Consumer<Throwable> failureHandler) {
        if (journaledBank.paused() || writer == null) throw new ForexException("Mercado pausado: avisá a un administrador.");
        try {
            ForexStore.Snapshot snapshot = ForexStore.capture(market);
            writer.submit(() -> ForexSettlement.persist(store, snapshot, journaledBank), failure -> {
                if (failure == null) {
                    try {
                        refreshCommittedViews();
                    } catch (RuntimeException problem) {
                        journaledBank.pause();
                        plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Forex] Could not refresh market views", problem);
                        failureHandler.accept(problem);
                        return;
                    }
                    success.run();
                } else {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "[Forex] Market paused; preserve forex/transfers.log and reconcile before restarting.", failure);
                    failureHandler.accept(failure);
                }
            });
        } catch (RuntimeException | Error failure) {
            journaledBank.pause();
            throw failure;
        }
    }

    /** Cross-module reads use the last durable state while a new snapshot is settling. */
    private void refreshCommittedViews() {
        committedViews = ForexCommittedViews.capture(market);
    }

    boolean committedClanPass(UUID player, String teamId) {
        return committedViews != null && committedViews.hasClanPass(player, teamId);
    }

    double committedCredit(UUID player) {
        return committedViews == null ? 1.0 : committedViews.creditMultiplier(player);
    }

    void saveNow(CommandSender sender) {
        if (!available(sender)) return;
        saveAsync(() -> sender.sendMessage(ForexAnnouncer.PREFIX + "§aMercado guardado."),
            failure -> sender.sendMessage(ForexAnnouncer.PREFIX + "§cMercado pausado: avisá a un administrador."));
    }

    void forceCycle(CommandSender sender) {
        adminMutation(sender, () -> market.forceCycle(), "§aCiclo de mercado forzado.");
    }

    void forceFlip(CommandSender sender, String ticker) {
        adminMutation(sender, () -> market.forceFlip(ticker), null);
    }

    private void adminMutation(CommandSender sender, Supplier<List<MarketEvent>> operation, String message) {
        runActor(sender, "admin market operation by " + sender.getName(), operation, events -> {
            events.forEach(announcer::announce);
            if (message != null) sendIfPresent(sender, message);
        });
    }

    private boolean available(CommandSender sender) {
        if (paused()) {
            sender.sendMessage(ForexAnnouncer.PREFIX + "§cMercado pausado: avisá a un administrador.");
            return false;
        }
        if (busy()) {
            sender.sendMessage(ForexAnnouncer.PREFIX + "§eEl mercado está guardando; intentá de nuevo en un momento.");
            return false;
        }
        return true;
    }

    boolean paused() { return journaledBank.paused(); }

    JournaledForexBank.ReconciliationDetails reconciliationDetails() {
        return journaledBank.reconciliationDetails();
    }

    String acknowledgeReconciliation(String journalHash, String marketHash, String operator) {
        java.nio.file.Path archive = journaledBank.acknowledgeReconciliation(journalHash, marketHash, operator);
        plugin.getLogger().warning("[Forex] " + operator + " acknowledged manual reconciliation; evidence: " + archive);
        return archive.toString();
    }

    /** Every second: market cycles, bubbles, flips and dividends on the leased worker. */
    void tickMarket() {
        if (paused() || busy() || writer == null) return;
        java.util.concurrent.atomic.AtomicReference<List<MarketEvent>> result = new java.util.concurrent.atomic.AtomicReference<>();
        try {
            writer.submit(() -> result.set(ForexTradeActor.tick(journaledBank, market, store)), failure -> {
                if (failure != null) {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "[Forex] Market tick paused; reconcile transfers.", failure);
                    return;
                }
                List<MarketEvent> events = result.get();
                if (!events.isEmpty()) {
                    try { refreshCommittedViews(); }
                    catch (RuntimeException problem) {
                        journaledBank.pause();
                        plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Forex] Could not refresh market views", problem);
                        return;
                    }
                    events.forEach(announcer::announce);
                }
            });
        } catch (RuntimeException failure) {
            journaledBank.pause();
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Forex] Could not start market tick", failure);
        }
    }

    /** Every tick: the timed sounds of sirens and funerals. */
    void tickEffects() {
        announcer.tick();
    }

    /** Emerald sparkles around the whales who did not hide them. */
    void tickAura() {
        if (busy() || paused() || committedViews == null) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (!committedViews.whales().contains(id) || market.auraHidden(id) || player.isInvisible()) {
                continue;
            }
            Location at = player.getLocation().add(0, 1.0, 0);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, at, 6, 0.4, 0.6, 0.4, 0);
        }
    }

    // ---------------------------------------------------------------- operations

    boolean buy(Player player, String asset, long money) {
        UUID id = player.getUniqueId();
        return runTrade(player, id, "buy " + asset + " " + money,
            () -> ForexMarket.isEmeraldAlias(asset) ? market.buyEmeralds(id, money) : market.buy(id, asset, money), true);
    }

    /** {@code quantity} receives the sellable balance and returns how much to sell. */
    boolean sell(Player player, String asset, DoubleUnaryOperator quantity) {
        if (!available(player)) return false;
        UUID id = player.getUniqueId();
        try {
            boolean emerald = ForexMarket.isEmeraldAlias(asset);
            double available = emerald ? market.emeralds(id) : market.coin(asset)
                .orElseThrow(() -> new ForexException("No existe $" + ForexMarket.normalizeTicker(asset) + "."))
                .free(id);
            double amount = quantity.applyAsDouble(available);
            return runTrade(player, id, "sell " + asset,
                () -> emerald ? market.sellEmeralds(id, amount) : market.sell(id, asset, amount), false);
        } catch (ForexException refusal) {
            player.sendMessage(ForexAnnouncer.PREFIX + "§c" + refusal.getMessage());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 0.7f, 1f);
            return false;
        }
    }

    /** Trades run on the leased writer; only the bridge executes Vault calls on the server thread. */
    private boolean runTrade(Player player, UUID id, String description, Supplier<TradeReceipt> operation, boolean buy) {
        return runActor(player, description + " player=" + id, operation, receipt -> {
            if (player.isOnline()) {
                ding(player, buy);
                player.sendMessage(ForexAnnouncer.PREFIX + tradeMessage(receipt, buy));
            }
        });
    }

    private <T> boolean runActor(CommandSender sender, String description, Supplier<T> operation,
                                 java.util.function.Consumer<T> onSuccess) {
        if (!available(sender)) return false;
        java.util.concurrent.atomic.AtomicReference<T> result = new java.util.concurrent.atomic.AtomicReference<>();
        try {
            writer.submit(() -> result.set(ForexTradeActor.settle(journaledBank, market, store,
                description, operation)), failure -> {
                if (failure == null) {
                    try {
                        refreshCommittedViews();
                    } catch (RuntimeException problem) {
                        journaledBank.pause();
                        plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Forex] Could not refresh market views", problem);
                        sendIfPresent(sender, "§cMercado pausado: avisá a un administrador.");
                        return;
                    }
                    try { onSuccess.accept(result.get()); }
                    catch (RuntimeException problem) {
                        plugin.getLogger().log(java.util.logging.Level.WARNING, "[Forex] Could not announce settled operation", problem);
                    }
                } else if (failure instanceof ForexException refusal && !journaledBank.paused()) {
                    sendIfPresent(sender, "§c" + refusal.getMessage());
                } else {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Forex] Trade paused; reconcile transfers.", failure);
                    sendIfPresent(sender, "§cMercado pausado: avisá a un administrador.");
                }
            });
            return true;
        } catch (RuntimeException failure) {
            journaledBank.pause();
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Forex] Could not start trade", failure);
            sendIfPresent(sender, "§cMercado pausado: avisá a un administrador.");
            return false;
        }
    }

    private void sendIfPresent(CommandSender sender, String message) {
        if (!(sender instanceof Player player) || player.isOnline()) {
            sender.sendMessage(ForexAnnouncer.PREFIX + message);
        }
    }

    private String tradeMessage(TradeReceipt receipt, boolean buy) {
        String message = (buy ? "§aCompraste §f" : "§aVendiste §f") + ForexFormat.tokens(receipt.quantity()) + " "
            + label(receipt.asset()) + " §apor §f" + ForexFormat.money(receipt.money()) + " §8(comisiones "
            + ForexFormat.money(receipt.fees()) + (buy ? ", precio " + ForexFormat.price(receipt.priceAfter()) : "") + ")";
        if (receipt.shortfall() > 0) {
            message += "\n" + ForexAnnouncer.PREFIX + "§c§l¡CORRIDA BANCARIA! §cEl pozo no alcanzó: te quedaron sin cobrar §f"
                + ForexFormat.money(receipt.shortfall()) + "§c.";
        }
        return message;
    }

    boolean stake(Player player, String ticker, DoubleUnaryOperator quantity) {
        return run(player, "stake " + ticker, () -> {
            Shitcoin coin = require(ticker);
            double staked = market.stake(player.getUniqueId(), ticker, quantity.applyAsDouble(coin.free(player.getUniqueId())));
            return "§aBloqueaste §f" + ForexFormat.tokens(staked) + " $" + coin.ticker() + "§a. Cobrás dividendos cada "
                + market.settings().coins().dividendMinutes() + " minutos. §8(bloqueo "
                + market.settings().coins().stakeLockMinutes() + "m)";
        });
    }

    boolean unstake(Player player, String ticker, DoubleUnaryOperator quantity) {
        return run(player, "unstake " + ticker, () -> {
            Shitcoin coin = require(ticker);
            double freed = market.unstake(player.getUniqueId(), ticker,
                quantity.applyAsDouble(coin.staked(player.getUniqueId())));
            return "§aDesbloqueaste §f" + ForexFormat.tokens(freed) + " $" + coin.ticker() + "§a.";
        });
    }

    boolean create(Player player, String ticker, String name) {
        UUID id = player.getUniqueId();
        return runActor(player, "create " + ticker + " player=" + id, () -> market.create(id, ticker, name), coin -> {
            announcer.launched(coin);
            if (player.isOnline()) player.sendMessage(ForexAnnouncer.PREFIX + "§a$" + coin.ticker()
                + " ya cotiza. Cobrás §f" + ForexFormat.percent(coin.creatorFee())
                + " §ade cada compra y venta. §7Ajustá con §f/forex comision§7, §f/forex dividendos §7y §f/forex clan§7.");
        });
    }

    private record RugOutcome(RugPull rug, Set<UUID> victims) { }

    boolean rugPull(Player player, String ticker) {
        UUID id = player.getUniqueId();
        return runActor(player, "rug pull " + ticker + " player=" + id, () -> {
            Shitcoin coin = require(ticker);
            Set<UUID> victims = new HashSet<>(coin.holdings().keySet());
            victims.remove(id);
            return new RugOutcome(market.rugPull(id, ticker), victims);
        }, result -> {
            announcer.rugPulled(result.rug(), result.victims());
            if (player.isOnline()) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 1f, 0.5f);
                player.sendMessage(ForexAnnouncer.PREFIX + "§2Te llevaste §f" + ForexFormat.money(result.rug().stolen())
                    + "§2. §7Don Casares ya anotó tu nombre.");
            }
        });
    }

    boolean setFee(Player player, String ticker, double fee) {
        return run(player, "set fee " + ticker, () -> {
            market.setCreatorFee(player.getUniqueId(), ticker, fee);
            return "§aComisión de $" + ForexMarket.normalizeTicker(ticker) + ": §f" + ForexFormat.percent(fee);
        });
    }

    boolean setStakingShare(Player player, String ticker, double share) {
        return run(player, "set staking share " + ticker, () -> {
            market.setStakingShare(player.getUniqueId(), ticker, share);
            return "§aDe tu comisión, §f" + ForexFormat.percent(share) + " §ava a los stakers de $"
                + ForexMarket.normalizeTicker(ticker) + ".";
        });
    }

    boolean linkClan(Player player, String ticker, boolean unlink) {
        return run(player, "link clan " + ticker, () -> {
            if (unlink) {
                market.linkTeam(player.getUniqueId(), ticker, null);
                return "§7$" + ForexMarket.normalizeTicker(ticker) + " ya no es la moneda de tu clan.";
            }
            String team = teams.ownedTeam(player.getUniqueId()).orElseThrow(() ->
                new ForexException("Tenés que ser el dueño de un clan."));
            market.linkTeam(player.getUniqueId(), ticker, team);
            String label = teams.label(team).orElse("tu clan");
            defer(() -> announcer.broadcast("§e$" + ForexMarket.normalizeTicker(ticker) + " §7es ahora la moneda oficial de "
                + label + "§7. Con §f" + ForexFormat.tokens(market.settings().clanPassThreshold())
                + " §7tokens tenés pase libre a sus cofres protegidos."));
            return "§aListo.";
        });
    }

    /** Hands out the live chart map of an asset; dead coins keep their last chart. */
    void giveChart(Player player, String rawAsset) {
        if (!available(player)) return;
        String asset = ChartFeed.asset(rawAsset);
        boolean known = ForexMarket.EMERALD_TICKER.equals(asset) || market.coin(asset).isPresent()
            || market.graveyard().stream().anyMatch(dead -> dead.ticker().equals(asset));
        if (!known) {
            player.sendMessage(ForexAnnouncer.PREFIX + "§cNo existe $" + asset + ".");
            return;
        }
        player.getInventory().addItem(charts.item(player, asset)).values()
            .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        player.sendMessage(ForexAnnouncer.PREFIX + "§aGráfico de " + label(asset)
            + " §aen vivo. §7Ponelo en marcos para armar tu pared de trading.");
        player.playSound(player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.MASTER, 0.8f, 1f);
    }

    void toggleAura(Player player) {
        run(player, "toggle aura", () -> {
            boolean shown = market.toggleAura(player.getUniqueId());
            return shown ? "§aTu aura de ballena está visible." : "§7Tu aura de ballena está oculta.";
        });
    }

    // ---------------------------------------------------------------- helpers

    String label(String asset) {
        return ForexMarket.EMERALD_TICKER.equals(asset) ? "§a$vEMR" : "§e$" + asset;
    }

    private Shitcoin require(String ticker) {
        return market.coin(ticker).orElseThrow(() ->
            new ForexException("No existe $" + ForexMarket.normalizeTicker(ticker) + "."));
    }

    private void defer(Runnable effect) {
        deferredEffects.add(effect);
    }

    private boolean run(Player player, String description, Supplier<String> operation) {
        if (!available(player)) return false;
        deferredEffects = new ArrayList<>();
        try {
            journaledBank.context(description + " player=" + player.getUniqueId());
            String message = operation.get();
            List<Runnable> effects = List.copyOf(deferredEffects);
            saveAsync(() -> {
                for (Runnable effect : effects) {
                    try { effect.run(); }
                    catch (RuntimeException failure) {
                        plugin.getLogger().log(java.util.logging.Level.WARNING, "[Forex] Could not announce settled operation", failure);
                    }
                }
                if (player.isOnline()) player.sendMessage(ForexAnnouncer.PREFIX + message);
            }, failure -> {
                if (player.isOnline()) player.sendMessage(ForexAnnouncer.PREFIX + "§cMercado pausado: avisá a un administrador.");
            });
            return true;
        } catch (ForexException refused) {
            if (journaledBank.hasUncommittedTransfer()) journaledBank.pause();
            player.sendMessage(ForexAnnouncer.PREFIX + "§c" + refused.getMessage());
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, SoundCategory.MASTER, 0.7f, 1f);
            return false;
        } catch (RuntimeException failure) {
            journaledBank.pause();
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "[Forex] Market paused; reconcile transfers.", failure);
            player.sendMessage(ForexAnnouncer.PREFIX + "§cMercado pausado: avisá a un administrador.");
            return false;
        } finally {
            deferredEffects = null;
        }
    }

    private static void ding(Player player, boolean buy) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 0.7f,
            buy ? 1.4f : 0.9f);
    }
}
