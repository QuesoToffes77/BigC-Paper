package dev.linqfy.bigCasares.modules.villagermood;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

/**
 * Infinite villager trades with consequences. Villagers boxed into 1x1 cells slowly get depressed and charge more,
 * squeeze out through 1x1 holes, may walk off cliffs, riot against nearby players, and insult players who spam
 * trades. Mood lives in each villager's persistent data, so it survives restarts.
 */
final class VillagerMoodRuntime {

    /** Riots and cliff walks are driven at this cadence. */
    static final long ACTION_INTERVAL_TICKS = 10L;
    private static final long PUNCH_COOLDOWN_TICKS = 30L;
    private static final double PUNCH_REACH = 2.2;
    private static final double ANNOUNCE_RADIUS = 16.0;

    private final VillagerMoodSettings settings;
    private final VillagerMoodPolicy policy;
    private final String namespace;
    private final NamespacedKey confinedKey;
    private final NamespacedKey annoyanceKey;
    private final NamespacedKey golemKey;
    private final DoubleSupplier random;
    private final LongSupplier clock;
    private final Consumer<Runnable> nextTick;
    private final TradeSpamTracker spamTracker;
    private final VillagerMafiaRuntime mafia;
    private final Map<UUID, Riot> riots = new HashMap<>();
    private final Queue<Riot> riotWork = new ArrayDeque<>();
    private final Map<UUID, Long> riotCooldownUntil = new HashMap<>();
    private final Map<UUID, CliffWalk> cliffWalks = new HashMap<>();
    private final LoadedVillagerIndex<Villager> loadedVillagers = new LoadedVillagerIndex<>();
    private final Queue<Chunk> seedChunks = new ArrayDeque<>();
    private final Queue<Iterator<Entity>> loadedEntityBatches = new ArrayDeque<>();
    private boolean loadedBatchTurn;
    private Entity[] seedEntities;
    private int seedCursor;
    private long moodCursor;
    private long moodLimit;
    private boolean moodScan;
    private boolean seedTurn;
    private PlayerScan playerScan;
    private final VillagerWorkBatch<Player> pendingPlayers = new VillagerWorkBatch<>();
    private final Map<UUID, Long> lastMoodCheck = new HashMap<>();
    private static final int VILLAGERS_PER_TICK = 32;
    private static final int PLAYERS_PER_TICK = 2;
    private static final int NEARBY_CANDIDATES_PER_TICK = 32;
    private static final int RIOT_ACTIONS_PER_TICK = 32;

    VillagerMoodRuntime(VillagerMoodSettings settings, NamespacedKey confinedKey, NamespacedKey annoyanceKey,
                        NamespacedKey golemKey, DoubleSupplier random, LongSupplier clock,
                        Consumer<Runnable> nextTick) {
        this.settings = settings;
        this.policy = new VillagerMoodPolicy(settings);
        this.namespace = confinedKey.getNamespace();
        this.confinedKey = confinedKey;
        this.annoyanceKey = annoyanceKey;
        this.golemKey = golemKey;
        this.random = random;
        this.clock = clock;
        this.nextTick = nextTick;
        this.spamTracker = new TradeSpamTracker(settings.spam().windowTicks(), settings.spam().maxTradesInWindow());
        this.mafia = new VillagerMafiaRuntime(settings.mafia(), confinedKey, this::isOrdinary, random);
    }

    // --- trading ---------------------------------------------------------------------------------

    boolean infiniteTrading() {
        return settings.trading().infinite();
    }

    /**
     * Runs when a trade window opens: restocks every offer and adds the mood surcharge. Vanilla resets the
     * special price when trading stops, so the surcharge never stacks between openings.
     */
    void onTradeOpen(Player player, MerchantInventory inventory) {
        if (!(inventory.getMerchant() instanceof AbstractVillager merchant) || !isOrdinary(merchant)) {
            return;
        }
        int depression = 0;
        int annoyance = 0;
        if (merchant instanceof Villager villager) {
            depression = policy.depressionLevel(read(villager, confinedKey));
            annoyance = policy.annoyanceLevel(read(villager, annoyanceKey));
        }
        List<MerchantRecipe> recipes = merchant.getRecipes();
        for (int index = 0; index < recipes.size(); index++) {
            MerchantRecipe recipe = recipes.get(index);
            if (settings.trading().refreshOnOpen()) {
                recipe.setUses(0);
            }
            List<ItemStack> ingredients = recipe.getIngredients();
            int baseCost = ingredients.isEmpty() || ingredients.getFirst() == null ? 0 : ingredients.getFirst().getAmount();
            int extra = policy.surcharge(baseCost, depression, annoyance);
            if (extra > 0) {
                recipe.setSpecialPrice(recipe.getSpecialPrice() + extra);
            }
            merchant.setRecipe(index, recipe);
        }
        double fraction = policy.surchargeFraction(depression, annoyance);
        if (fraction > 0.0) {
            String reason = depression > 0 && annoyance > 0 ? "deprimido y harto de vos"
                : depression > 0 ? "deprimido (depresión " + depression + "/" + settings.depression().maxLevel() + ")"
                : "harto de vos";
            player.sendActionBar(ChatColor.GOLD + "El aldeano está " + reason + ": precios +"
                + Math.round(fraction * 100) + "%");
        }
    }

    /** Runs for every completed trade with a villager or wandering trader. */
    void onTrade(Player player, AbstractVillager merchant) {
        if (!(merchant instanceof Villager villager) || !isOrdinary(villager)) {
            return;
        }
        if (spamTracker.record(player.getUniqueId(), villager.getUniqueId(), clock.getAsLong())) {
            insult(player, villager);
        }
    }

    private void insult(Player player, Villager villager) {
        write(villager, annoyanceKey, policy.annoy(read(villager, annoyanceKey)));
        player.sendMessage(speaker() + pick(settings.insults()));
        villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
        villager.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, villager.getEyeLocation(), 5, 0.3, 0.3, 0.3);
        player.sendActionBar(ChatColor.RED + "Te subió los precios por pesado ("
            + policy.annoyanceLevel(read(villager, annoyanceKey)) + "/" + settings.spam().maxLevel() + ")");
        // Close the window so the new prices apply when it is reopened.
        nextTick.accept(() -> {
            InventoryView view = player.getOpenInventory();
            if (view.getTopInventory() instanceof MerchantInventory open
                && open.getMerchant() instanceof Entity entity
                && entity.getUniqueId().equals(villager.getUniqueId())) {
                player.closeInventory();
            }
        });
    }

    // --- periodic mood check ---------------------------------------------------------------------

    /** Runs every {@code check-interval-ticks}. */
    void check(Iterable<? extends Player> players) {
        long now = clock.getAsLong();
        spamTracker.cleanup(now);
        riotCooldownUntil.values().removeIf(until -> until <= now);
        // Advance mood independently of slow nearby-player scans, without copying world entities.
        if (!moodScan) {
            moodCursor = 0L;
            moodLimit = loadedVillagers.watermark();
            moodScan = true;
        }
        if ((settings.riot().enabled() || settings.mafia().enabled())
            && playerScan == null && pendingPlayers.isEmpty()) {
            List<Player> playerBatch = new ArrayList<>();
            players.forEach(playerBatch::add);
            pendingPlayers.addAll(playerBatch);
        }
    }

    /** Apply a bounded portion of the current snapshot each tick. */
    void tickChecks() {
        long now = clock.getAsLong();
        boolean hasPlayerWork = playerScan != null || !pendingPlayers.isEmpty();
        boolean hasSeedWork = seedEntities != null || !seedChunks.isEmpty() || !loadedEntityBatches.isEmpty();
        boolean seedThisTick = hasSeedWork && (!hasPlayerWork || (seedTurn = !seedTurn));
        if (seedThisTick) seedOneChunk();
        for (int i = 0; i < VILLAGERS_PER_TICK && moodScan; i++) {
            var next = loadedVillagers.next(moodCursor, moodLimit);
            if (next.isEmpty()) {
                moodScan = false;
                break;
            }
            moodCursor = next.get().sequence();
            Villager villager = next.get().value();
            if (!isOrdinary(villager)) {
                lastMoodCheck.remove(villager.getUniqueId());
                continue;
            }
            Long previous = lastMoodCheck.put(villager.getUniqueId(), now);
            long elapsed = previous == null ? settings.checkIntervalTicks() : Math.max(1L, now - previous);
            updateMood(villager, new BukkitBlockProbe(villager.getWorld()), elapsed);
        }
        tickPlayerScans(now, seedThisTick);
    }

    /** One loaded chunk snapshot per tick, and a bounded number of entities or candidates. */
    private void tickPlayerScans(long now, boolean snapshotTaken) {
        int budget = NEARBY_CANDIDATES_PER_TICK;
        int completed = 0;
        while (budget > 0 && completed < PLAYERS_PER_TICK) {
            if (playerScan == null) {
                Player player = pendingPlayers.poll();
                if (player == null) return;
                if (!player.isOnline()) { completed++; continue; }
                playerScan = new PlayerScan(player);
            }
            PlayerScan scan = playerScan;
            if (!scan.player.isOnline() || scan.player.getWorld() != scan.center.getWorld()) {
                playerScan = null;
                completed++;
                continue;
            }
            if (!scan.validating) {
                if (scan.chunkEntities != null && scan.entityCursor < scan.chunkEntities.length) {
                    budget--;
                    Entity entity = scan.chunkEntities[scan.entityCursor++];
                    if (entity instanceof Villager villager && scan.seen.add(villager.getUniqueId())) {
                        scan.nearby.add(villager);
                    }
                    continue;
                }
                scan.chunkEntities = null;
                if (scan.chunkX > scan.maxChunkX) {
                    scan.validating = true;
                    scan.validationCenter = scan.player.getLocation().clone();
                    continue;
                }
                if (snapshotTaken) return;
                int chunkX = scan.chunkX;
                int chunkZ = scan.chunkZ;
                if (++scan.chunkZ > scan.maxChunkZ) {
                    scan.chunkZ = scan.minChunkZ;
                    scan.chunkX++;
                }
                // Count even an unloaded chunk against the per-tick traversal budget.
                snapshotTaken = true;
                World world = scan.center.getWorld();
                if (world != null && world.isChunkLoaded(chunkX, chunkZ)) {
                    scan.chunkEntities = world.getChunkAt(chunkX, chunkZ).getEntities();
                    scan.entityCursor = 0;
                }
                continue;
            }
            if (scan.validationCursor >= scan.nearby.size()) {
                if (scan.player.getLocation().distanceSquared(scan.validationCenter) > 64.0) {
                    playerScan = null;
                    completed++;
                    continue;
                }
                if (scan.riotEligible && scan.rioters.size() >= settings.riot().minRioters()
                    && random.getAsDouble() < settings.riot().chancePerCheck()) {
                    startRiot(scan.player, scan.rioters);
                }
                mafia.consider(scan.player, scan.sources, now);
                playerScan = null;
                completed++;
                continue;
            }
            budget--;
            Villager villager = scan.nearby.get(scan.validationCursor++);
            if (!isOrdinary(villager) || villager.getWorld() != scan.player.getWorld()) continue;
            Location at = villager.getLocation();
            Location playerAt = scan.player.getLocation();
            double dx = Math.abs(at.getX() - playerAt.getX());
            double dy = Math.abs(at.getY() - playerAt.getY());
            double dz = Math.abs(at.getZ() - playerAt.getZ());
            double riotRadius = settings.riot().radius();
            if (scan.riotEligible && dx <= riotRadius && dy <= riotRadius && dz <= riotRadius
                && policy.joinsRiot(policy.depressionLevel(read(villager, confinedKey)),
                    policy.annoyanceLevel(read(villager, annoyanceKey)))) scan.rioters.add(villager);
            double mafiaRadius = settings.mafia().radius();
            if (settings.mafia().enabled() && VillagerMafiaRuntime.canTarget(scan.player)
                && dx * dx + dy * dy + dz * dz <= mafiaRadius * mafiaRadius) {
                scan.sources.add(mafia.source(villager));
            }
        }
    }

    private void updateMood(Villager villager, BlockProbe probe, long interval) {
        Location at = villager.getLocation();
        int x = at.getBlockX();
        int y = at.getBlockY();
        int z = at.getBlockZ();
        boolean inVehicle = villager.isInsideVehicle();
        boolean confined = inVehicle || VillagerMoodPolicy.isConfined(probe, x, y, z);

        long before = read(villager, confinedKey);
        long confinedTicks = policy.nextConfinedTicks(before, confined, interval);
        write(villager, confinedKey, confinedTicks);
        write(villager, annoyanceKey, policy.decayAnnoyance(read(villager, annoyanceKey), interval));

        int previousLevel = policy.depressionLevel(before);
        int level = policy.depressionLevel(confinedTicks);
        if (level > previousLevel) {
            villager.getWorld().playSound(at, Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 0.5f);
            announce(at, ChatColor.GRAY + "Un aldeano se siente encerrado... (depresión " + level + "/"
                + settings.depression().maxLevel() + ")");
        }
        if (level > 0 && random.getAsDouble() < 0.5) {
            villager.getWorld().spawnParticle(Particle.FALLING_WATER, villager.getEyeLocation(), 3, 0.15, 0.05, 0.15);
        }

        VillagerMoodSettings.Escape escape = settings.escape();
        if (confined && !inVehicle && escape.enabled() && confinedTicks >= escape.minConfinedTicks()
            && random.getAsDouble() < escape.chancePerCheck()) {
            VillagerMoodPolicy.findEscapeHole(probe, x, y, z).ifPresent(cell -> escape(villager, cell));
            return;
        }
        VillagerMoodSettings.Cliff cliff = settings.cliff();
        if (!confined && cliff.enabled() && level >= cliff.minDepression()
            && !cliffWalks.containsKey(villager.getUniqueId()) && random.getAsDouble() < cliff.chancePerCheck()) {
            VillagerMoodPolicy.findCliffEdge(probe, x, y, z, cliff.searchRadius(), cliff.minDrop())
                .ifPresent(cell -> startCliffWalk(villager, cell));
        }
    }

    private void escape(Villager villager, VillagerMoodPolicy.Cell cell) {
        Location from = villager.getLocation();
        Location to = new Location(villager.getWorld(), cell.x() + 0.5, cell.y(), cell.z() + 0.5,
            from.getYaw(), from.getPitch());
        villager.getWorld().spawnParticle(Particle.POOF, from.clone().add(0, 1, 0), 10, 0.2, 0.4, 0.2, 0.02);
        villager.teleport(to);
        villager.getWorld().playSound(to, Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.2f);
        announce(to, ChatColor.YELLOW + "¡Un aldeano se escapó por un agujero de la celda!");
    }

    // --- cliff -----------------------------------------------------------------------------------

    private void startCliffWalk(Villager villager, VillagerMoodPolicy.Cell cell) {
        Location edge = new Location(villager.getWorld(), cell.x() + 0.5, cell.y(), cell.z() + 0.5);
        cliffWalks.put(villager.getUniqueId(), new CliffWalk(villager, edge, settings.cliff().attemptTicks()));
        villager.getPathfinder().moveTo(edge, 0.6);
        announce(villager.getLocation(), ChatColor.DARK_GRAY + "Un aldeano deprimido camina hacia el precipicio...");
    }

    /** Runs every {@link #ACTION_INTERVAL_TICKS}: cliff walks and riots. */
    void tickActions() {
        tickCliffWalks();
        tickRiots();
        mafia.tick(clock.getAsLong());
    }

    private void tickCliffWalks() {
        Iterator<CliffWalk> iterator = cliffWalks.values().iterator();
        while (iterator.hasNext()) {
            CliffWalk walk = iterator.next();
            Villager villager = walk.villager;
            walk.remainingTicks -= ACTION_INTERVAL_TICKS;
            if (!villager.isValid() || walk.remainingTicks <= 0 || villager.getWorld() != walk.edge.getWorld()) {
                iterator.remove();
                continue;
            }
            Location at = villager.getLocation();
            double dx = walk.edge.getX() - at.getX();
            double dz = walk.edge.getZ() - at.getZ();
            if (dx * dx + dz * dz <= 2.5) {
                Vector push = new Vector(dx, 0, dz);
                if (push.lengthSquared() > 0.0) {
                    push.normalize().multiply(0.45);
                }
                villager.setVelocity(push.setY(0.25));
                villager.getWorld().playSound(at, Sound.ENTITY_VILLAGER_HURT, 1.0f, 0.6f);
                iterator.remove();
                continue;
            }
            villager.getPathfinder().moveTo(walk.edge, 0.6);
        }
    }

    // --- riots -----------------------------------------------------------------------------------

    private void startRiot(Player player, List<Villager> rioters) {
        if (rioters.isEmpty() || !player.isOnline() || !player.isValid()
            || riots.containsKey(player.getUniqueId()) || riotCooldownUntil.containsKey(player.getUniqueId())
            || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR
            || !isOrdinary(rioters.getFirst()) || rioters.getFirst().getWorld() != player.getWorld()) return;
        Location first = rioters.getFirst().getLocation();
        Location target = player.getLocation();
        double radius = settings.riot().radius();
        if (Math.abs(first.getX() - target.getX()) > radius || Math.abs(first.getY() - target.getY()) > radius
            || Math.abs(first.getZ() - target.getZ()) > radius) return;
        IronGolem golem = null;
        if (settings.riot().summonGolem()) {
            Location spawn = rioters.getFirst().getLocation();
            golem = spawn.getWorld().spawn(spawn, IronGolem.class, created -> {
                created.getPersistentDataContainer().set(golemKey, PersistentDataType.BYTE, (byte) 1);
                created.setPlayerCreated(false);
                created.setPersistent(false);
            });
            golem.setTarget(player);
        }
        Riot riot = new Riot(player, rioters, golem, clock.getAsLong() + settings.riot().durationTicks());
        riots.put(player.getUniqueId(), riot);
        riotWork.add(riot);
        player.sendTitle(ChatColor.RED + "¡MOTÍN!", ChatColor.GOLD + "Los aldeanos se rebelan contra vos", 5, 50, 10);
        player.sendMessage(speaker() + pick(settings.riotShouts()));
    }

    private void tickRiots() {
        long now = clock.getAsLong();
        for (int step = 0; step < RIOT_ACTIONS_PER_TICK; step++) {
            Riot riot = riotWork.poll();
            if (riot == null) return;
            Player player = riot.player;
            if (riots.get(player.getUniqueId()) != riot) continue;
            if (!player.isOnline() || player.isDead() || now >= riot.expiresAt) {
                riots.remove(player.getUniqueId());
                endRiot(riot, now);
                continue;
            }
            if (riot.pulse != now) {
                riot.pulse = now;
                riot.remainingThisPulse = riot.rioters.size();
                if (riot.golem != null && riot.golem.isValid()) riot.golem.setTarget(player);
            }
            if (riot.remainingThisPulse > 0 && !riot.rioters.isEmpty()) {
                Villager rioter = riot.rioters.get(riot.nextRioter);
                riot.nextRioter = (riot.nextRioter + 1) % riot.rioters.size();
                riot.remainingThisPulse--;
                if (rioter.isValid() && rioter.getWorld() == player.getWorld()) {
                    if (riot.initialSoundsRemaining > 0) {
                        rioter.getWorld().playSound(rioter.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.7f);
                    }
                    rioter.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, rioter.getEyeLocation(), 1, 0.3, 0.3, 0.3);
                    double distance = rioter.getLocation().distance(player.getLocation());
                    Long lastPunch = riot.lastPunch.get(rioter.getUniqueId());
                    if (distance <= PUNCH_REACH && (lastPunch == null || now - lastPunch >= PUNCH_COOLDOWN_TICKS)) {
                        riot.lastPunch.put(rioter.getUniqueId(), now);
                        player.damage(settings.riot().damage(), rioter);
                        Vector knockback = player.getLocation().toVector().subtract(rioter.getLocation().toVector()).setY(0);
                        if (knockback.lengthSquared() > 0.0) {
                            player.setVelocity(player.getVelocity().add(knockback.normalize().multiply(0.3).setY(0.2)));
                        }
                    } else if (distance > PUNCH_REACH && distance <= settings.riot().radius() * 2) {
                        rioter.getPathfinder().moveTo(player, 0.7);
                    }
                    if (random.getAsDouble() < 0.04) player.sendMessage(speaker() + pick(settings.riotShouts()));
                }
                if (riot.initialSoundsRemaining > 0) riot.initialSoundsRemaining--;
            }
            riotWork.add(riot);
        }
    }

    private void endRiot(Riot riot, long now) {
        if (riot.golem != null && riot.golem.isValid()) {
            riot.golem.remove();
        }
        riotCooldownUntil.put(riot.player.getUniqueId(), now + settings.riot().cooldownTicks());
        if (riot.player.isOnline()) {
            riot.player.sendMessage(ChatColor.GRAY + "El motín de los aldeanos se calmó... por ahora.");
        }
    }

    boolean isRiotGolem(Entity entity) {
        return entity instanceof IronGolem && entity.getPersistentDataContainer().has(golemKey, PersistentDataType.BYTE);
    }

    // --- lifecycle -------------------------------------------------------------------------------

    void forget(UUID playerId) {
        mafia.forget(playerId);
        spamTracker.forgetPlayer(playerId);
        Riot riot = riots.remove(playerId);
        if (riot != null && riot.golem != null && riot.golem.isValid()) {
            riot.golem.remove();
        }
    }

    void shutdown() {
        mafia.shutdown();
        loadedVillagers.clear();
        seedChunks.clear();
        loadedEntityBatches.clear();
        seedEntities = null;
        moodScan = false;
        playerScan = null;
        pendingPlayers.clear();
        lastMoodCheck.clear();
        for (Riot riot : riots.values()) {
            if (riot.golem != null && riot.golem.isValid()) {
                riot.golem.remove();
            }
        }
        riots.clear();
        riotWork.clear();
        riotCooldownUntil.clear();
        cliffWalks.clear();
        spamTracker.clear();
    }

    void seedLoadedChunks(Chunk[] chunks) {
        for (Chunk chunk : chunks) seedChunks.add(chunk);
    }

    void entitiesLoaded(List<Entity> entities) {
        if (!entities.isEmpty()) loadedEntityBatches.add(entities.iterator());
    }

    private void seedOneChunk() {
        boolean haveChunks = seedEntities != null || !seedChunks.isEmpty();
        if (!loadedEntityBatches.isEmpty() && (!haveChunks || (loadedBatchTurn = !loadedBatchTurn))) {
            Iterator<Entity> batch = loadedEntityBatches.peek();
            for (int i = 0; i < VILLAGERS_PER_TICK && batch.hasNext(); i++) {
                Entity entity = batch.next();
                if (entity.isValid()) removeMafiaOrphan(entity);
            }
            if (!batch.hasNext()) loadedEntityBatches.remove();
            return;
        }
        if (seedEntities == null) {
            Chunk chunk = seedChunks.poll();
            if (chunk == null || !chunk.isLoaded()) return;
            seedEntities = chunk.getEntities();
            seedCursor = 0;
        }
        int end = Math.min(seedEntities.length, seedCursor + VILLAGERS_PER_TICK);
        while (seedCursor < end) {
            Entity entity = seedEntities[seedCursor++];
            // The chunk may have unloaded after its snapshot was taken.
            if (!entity.isValid()) continue;
            removeMafiaOrphan(entity);
            entityAdded(entity);
        }
        if (seedCursor == seedEntities.length) seedEntities = null;
    }

    void entityAdded(Entity entity) {
        if (entity instanceof Villager villager) loadedVillagers.add(villager.getUniqueId(), villager);
    }

    void entityRemoved(Entity entity) {
        if (entity instanceof Villager villager && loadedVillagers.remove(villager.getUniqueId(), villager)) {
            lastMoodCheck.remove(villager.getUniqueId());
        }
    }

    boolean isMafiaEntity(Entity entity) {
        return mafia.owns(entity);
    }

    void onMafiaDamage(EntityDamageByEntityEvent event) {
        mafia.onDamage(event);
    }

    void removeMafiaOrphan(Entity entity) {
        mafia.removeOrphan(entity);
    }

    // --- helpers ---------------------------------------------------------------------------------

    /**
     * Ordinary villagers only: plugin NPCs (shop keepers, Don Pollos, Sift singers...) are invulnerable, have no AI
     * or carry this plugin's own persistent markers, and must keep their scripted behaviour.
     */
    boolean isOrdinary(AbstractVillager merchant) {
        if (!merchant.isValid() || merchant.isInvulnerable() || !merchant.hasAI()) {
            return false;
        }
        if (merchant instanceof Villager villager && !villager.isAdult()) {
            return false;
        }
        Set<NamespacedKey> keys = merchant.getPersistentDataContainer().getKeys();
        for (NamespacedKey key : keys) {
            if (key.getNamespace().equals(namespace) && !key.equals(confinedKey) && !key.equals(annoyanceKey)) {
                return false;
            }
        }
        return true;
    }

    private static long read(Entity entity, NamespacedKey key) {
        Long value = entity.getPersistentDataContainer().get(key, PersistentDataType.LONG);
        return value == null ? 0L : value;
    }

    private static void write(Entity entity, NamespacedKey key, long value) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        if (value <= 0L) {
            container.remove(key);
        } else {
            container.set(key, PersistentDataType.LONG, value);
        }
    }

    private void announce(Location at, String message) {
        World world = at.getWorld();
        if (world == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(at) <= ANNOUNCE_RADIUS * ANNOUNCE_RADIUS) {
                player.sendMessage(message);
            }
        }
    }

    private String pick(List<String> lines) {
        int index = (int) Math.min(lines.size() - 1, Math.floor(random.getAsDouble() * lines.size()));
        return lines.get(Math.max(0, index));
    }

    private static String speaker() {
        return ChatColor.GREEN + "[Aldeano] " + ChatColor.WHITE;
    }

    private final class PlayerScan {
        private final Player player;
        private final Location center;
        private final int minChunkZ, maxChunkX, maxChunkZ;
        private int chunkX, chunkZ;
        private Entity[] chunkEntities;
        private int entityCursor;
        private final Set<UUID> seen = new HashSet<>();
        private final List<Villager> nearby = new ArrayList<>();
        private boolean validating;
        private Location validationCenter;
        private int validationCursor;
        private final boolean riotEligible;
        private final List<Villager> rioters = new ArrayList<>();
        private final List<VillagerMafiaService.Source> sources = new ArrayList<>();

        private PlayerScan(Player player) {
            this.player = player;
            this.center = player.getLocation().clone();
            // Include a small movement margin; candidates are checked against the current position later.
            double riotRadius = settings.riot().enabled() ? settings.riot().radius() : 0.0;
            double mafiaRadius = settings.mafia().enabled() ? settings.mafia().radius() : 0.0;
            double radius = Math.max(riotRadius, mafiaRadius) + 8.0;
            this.chunkX = (int) Math.floor((center.getX() - radius) / 16.0);
            this.maxChunkX = (int) Math.floor((center.getX() + radius) / 16.0);
            this.minChunkZ = (int) Math.floor((center.getZ() - radius) / 16.0);
            this.maxChunkZ = (int) Math.floor((center.getZ() + radius) / 16.0);
            this.chunkZ = minChunkZ;
            UUID id = player.getUniqueId();
            this.riotEligible = settings.riot().enabled() && !riots.containsKey(id)
                && !riotCooldownUntil.containsKey(id) && player.isValid()
                && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR;
        }
    }

    private static final class CliffWalk {
        private final Villager villager;
        private final Location edge;
        private long remainingTicks;

        private CliffWalk(Villager villager, Location edge, long remainingTicks) {
            this.villager = villager;
            this.edge = edge;
            this.remainingTicks = remainingTicks;
        }
    }

    private static final class Riot {
        private final Player player;
        private final List<Villager> rioters;
        private final IronGolem golem;
        private final Map<UUID, Long> lastPunch = new HashMap<>();
        private final long expiresAt;
        private long pulse = Long.MIN_VALUE;
        private int remainingThisPulse;
        private int nextRioter;
        private int initialSoundsRemaining;

        private Riot(Player player, List<Villager> rioters, IronGolem golem, long expiresAt) {
            this.player = player;
            this.rioters = rioters;
            this.golem = golem;
            this.expiresAt = expiresAt;
            this.initialSoundsRemaining = rioters.size();
        }
    }
}
