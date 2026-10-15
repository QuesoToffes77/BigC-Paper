package dev.linqfy.bigCasares.modules.donpollos;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dev.linqfy.bigCasares.modules.donpollos.DonPollosBossSupport.*;

/**
 * Don Pollo Boss: una estatua de Don Pollo sobre un auto (modelo bigcasares_don_pollo_boss) montada
 * en un Ravager, que embiste como un auto. Cada tanto frena, mira a un jugador y le tira dos rayos
 * laser por los ojos. Tiene barra de boss y suelta premio al morir.
 *
 * <p>Pelea por fases ({@link DonPollosBossPhase}): en la FASE 1 te persigue a velocidad de bebe zombie;
 * en la FASE 2 se aleja y te mantiene a distancia, tira meteoritos del logo de WhatsApp y spawnea
 * gallinas WhatsApp; en la FASE 3 se eleva ("No hay picante para ti..."), vacia el chunk de cada jugador
 * e invoca Labubus y al Coronel. Los bichos invocados los maneja {@link DonPollosBossMinions}.
 */
final class DonPollosBossManager implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    static final String MODEL = "bigcasares_don_pollo_boss";
    /** Velocidad de un bebe zombie (zombie 0.23 + 50% por ser bebe). */
    private static final double BABY_ZOMBIE_SPEED = 0.345;
    /** El Ravager lleva solo su velocidad base a 0.35 cuando persigue (y a 0.3 sin objetivo). */
    private static final double RAVAGER_CHASE_SPEED = 0.35;
    private static final List<DonPollosBossPhase> PHASES = DonPollosBossPhase.defaults();

    private final JavaPlugin plugin;
    private final DonPollosModelBinder binder;
    private final DonPollosSounds sounds;
    private final DonPollosBossSettings settings;
    private final NamespacedKey bossKey;
    /** Dificultad con la que salio este boss (victorias de quien lo invoco). */
    private final NamespacedKey difficultyKey;
    private final DonPollosBossMinions minions;
    private final Map<UUID, DonPollosBoss> bosses = new HashMap<>();
    private final DonPollosBossAttacks attacks;
    private final DonPollosBossTerrain terrain;
    private final DonPollosCubeBattles cubes;
    private final DonPollosBossDefeat defeat;
    private BukkitTask task;

    DonPollosBossManager(JavaPlugin plugin, DonPollosModelBinder binder, DonPollosSounds sounds,
                         DonPollosBossSettings settings, DonPollosProtection protection) {
        this.plugin = plugin;
        this.binder = binder;
        this.sounds = sounds;
        this.settings = settings;
        this.bossKey = new NamespacedKey(plugin, "don_pollo_boss");
        this.difficultyKey = new NamespacedKey(plugin, "boss_dificultad");
        this.minions = new DonPollosBossMinions(plugin, binder, settings);
        this.attacks = new DonPollosBossAttacks(plugin, binder, settings, minions);
        this.terrain = new DonPollosBossTerrain(bosses, protection, player -> {
            player.sendActionBar(LEGACY.deserialize("§cNo hay escape del cubo hasta ganar las Batallas del Cubo"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 0.8f);
        }, player -> player.sendActionBar(
            LEGACY.deserialize("§cNo podes poner ni romper bloques en el cubo hasta ganar las Batallas del Cubo")));
        this.cubes = new DonPollosCubeBattles(plugin, settings, minions, terrain);
        this.defeat = new DonPollosBossDefeat(plugin, sounds, bosses, minions, attacks, cubes, this::untrack);
    }

    void start() {
        for (World world : Bukkit.getWorlds()) {
            for (Ravager ravager : world.getEntitiesByClass(Ravager.class)) {
                if (isBoss(ravager)) {
                    track(ravager);
                }
            }
        }
        plugin.getServer().getPluginManager().registerEvents(minions, plugin);
        plugin.getServer().getPluginManager().registerEvents(terrain, plugin);
        defeat.start();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    /** Un Balde de KFC dado vuelta (el premio fijo), para /donpollo give balde. */
    ItemStack kfcBucket() {
        return defeat.kfcBucket();
    }

    /** Cierra los menus del premio mientras su listener todavia los protege. */
    void closeMenus() {
        defeat.closeMenus();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        attacks.deathMessage(event);
    }

    void shutdown() {
        if (task != null) {
            task.cancel();
        }
        attacks.clearProjectiles();
        defeat.stop();
        org.bukkit.event.HandlerList.unregisterAll(minions);
        org.bukkit.event.HandlerList.unregisterAll(terrain);
        for (UUID id : new ArrayList<>(bosses.keySet())) {
            DonPollosBoss boss = bosses.get(id);
            if (boss != null && boss.ravager.isValid()) {
                boss.ravager.setAI(true);
                boss.ravager.setGravity(true);
                attacks.setInflate(boss.ravager, 0);
            }
            untrack(id);
        }
    }

    Ravager spawn(Location location) {
        return spawn(location, null);
    }

    /**
     * El boss sale mas fuerte segun cuantas veces ya le gano quien lo invoca (sin jugador, como la primera vez).
     * La dificultad queda guardada en el boss, asi no cambia si se descarga el chunk o se reinicia el servidor.
     */
    Ravager spawn(Location location, Player summoner) {
        int level = summoner == null ? 0 : defeat.bossWins(summoner);
        DonPollosBossDifficulty difficulty = new DonPollosBossDifficulty(level);
        Ravager ravager = location.getWorld().spawn(location, Ravager.class, boss -> {
            boss.getPersistentDataContainer().set(bossKey, PersistentDataType.BYTE, (byte) 1);
            boss.getPersistentDataContainer().set(difficultyKey, PersistentDataType.INTEGER, level);
            boss.customName(LEGACY.deserialize("§4§lDon Pollo Boss"));
            boss.setCustomNameVisible(false);
            boss.setPersistent(true);
            boss.setRemoveWhenFarAway(false);
            boss.setCanJoinRaid(false);
            setAttribute(boss, Attribute.MAX_HEALTH, settings.health());
            setAttribute(boss, Attribute.SCALE, settings.scale());
            setAttribute(boss, Attribute.ATTACK_DAMAGE, difficulty.damage(settings.meleeDamage()));
            setAttribute(boss, Attribute.KNOCKBACK_RESISTANCE, 1.0);
            setAttribute(boss, Attribute.FOLLOW_RANGE, CHASE_RANGE);
            boss.setHealth(settings.health());
        });
        track(ravager);
        sounds.play(ravager);
        ravager.getWorld().playSound(ravager.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 2f, 0.7f);
        if (summoner != null) {
            summoner.sendMessage(LEGACY.deserialize(DonPollosBossDifficulty.summonMessage(level)));
        }
        return ravager;
    }

    boolean isBoss(Entity entity) {
        return entity instanceof Ravager
            && entity.getPersistentDataContainer().has(bossKey, PersistentDataType.BYTE);
    }

    void track(Ravager ravager) {
        if (bosses.containsKey(ravager.getUniqueId())) {
            return;
        }
        BossBar bar = BossBar.bossBar(barTitle(PHASES.getFirst()), 1f,
            BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        DonPollosBoss boss = new DonPollosBoss(ravager);
        boss.bar = bar;
        boss.difficulty = new DonPollosBossDifficulty(
            ravager.getPersistentDataContainer().getOrDefault(difficultyKey, PersistentDataType.INTEGER, 0));
        ravager.setGravity(true);
        ravager.setAI(true);
        attacks.setInflate(ravager, 0);
        boss.nextLaser = Bukkit.getCurrentTick() + boss.difficulty.cooldownTicks(settings.laserCooldownSeconds() * 20.0);
        bosses.put(ravager.getUniqueId(), boss);
        binder.bindModel(ravager, MODEL);
        defeat.restore(boss);
    }

    void untrack(UUID id) {
        DonPollosBoss boss = bosses.remove(id);
        if (boss != null) {
            for (UUID viewer : boss.viewers) {
                Player player = Bukkit.getPlayer(viewer);
                if (player != null) {
                    player.hideBossBar(boss.bar);
                }
            }
            binder.release(id);
            minions.removeAll(boss.minions);
            cubes.endCube(boss);
        }
    }

    void tick() {
        long now = Bukkit.getCurrentTick();
        attacks.tickProjectiles();
        for (DonPollosBoss boss : new ArrayList<>(bosses.values())) {
            Ravager ravager = boss.ravager;
            if (!ravager.isValid()) {
                continue;
            }
            updateBar(boss);
            if (boss.defeat != null) {
                defeat.tickDefeat(boss, now);
                continue;
            }
            updatePhase(boss);
            if (boss.cube != null) {
                cubes.tickCube(boss, now);
            }
            if (now % 20 == 0) {
                minions.tick(boss.minions);
            }
            if (boss.levitateEnd > 0) {
                cubes.levitate(boss, now);
                continue;
            }
            if (boss.hoverAt != null) {
                cubes.hover(boss, now);
                continue;
            }
            if (boss.phase.has(DonPollosBossPhase.Ability.EYE_LASERS) && now % 2 == 0) {
                attacks.eyeLasers(boss, now);
            }
            if (boss.jumpEnd > 0) {
                attacks.tickJump(boss, now);
                continue;
            }
            if (boss.beamStart > 0) {
                attacks.tickBeam(boss, now);
                continue;
            }
            if (boss.firingUntil > 0) {
                attacks.fireLaser(boss, now);
                continue;
            }
            if (now % 10 == 0) {
                if (DonPollosBossPhase.KEEP_AWAY.equals(boss.phase.ai())) {
                    attacks.keepAway(boss, now);
                } else {
                    attacks.chase(boss);
                }
            }
            if (boss.phase.laser() && now >= boss.nextLaser) {
                attacks.startLaser(boss, now);
            }
            if (boss.phase.meteors() && now >= boss.nextMeteor) {
                attacks.launchMeteor(boss, now);
            }
            if (boss.phase.chickens() && now >= boss.nextChickens) {
                attacks.spawnChickens(boss);
                boss.nextChickens = now + boss.difficulty.cooldownTicks(settings.chickenWaveSeconds() * 20);
            }
            if (boss.phase.has(DonPollosBossPhase.Ability.HOP)) {
                attacks.hop(boss, now);
            }
            if (boss.phase.has(DonPollosBossPhase.Ability.WARDEN_BEAM) && now >= boss.nextBeam) {
                attacks.startBeam(boss, now);
            } else if (boss.phase.has(DonPollosBossPhase.Ability.MEGA_JUMP) && now >= boss.nextJump) {
                attacks.startJump(boss, now);
            }
            if (boss.phase.has(DonPollosBossPhase.Ability.FINAL_COLONELS) && now >= boss.nextFinalColonels) {
                attacks.spawnFinalColonels(boss);
                boss.nextFinalColonels = now + boss.difficulty.cooldownTicks(settings.phase4().colonelSeconds() * 20);
            }
        }
    }

    static Component barTitle(DonPollosBossPhase phase) {
        return LEGACY.deserialize("§4§lDON POLLO BOSS §8| §c§l" + phase.title());
    }

    /** Cambia de fase segun la vida y lo anuncia; la fase define como se mueve y si tira laser. */
    void updatePhase(DonPollosBoss boss) {
        Ravager ravager = boss.ravager;
        double fraction = ravager.getHealth() / ravager.getAttribute(Attribute.MAX_HEALTH).getValue();
        DonPollosBossPhase phase = DonPollosBossPhase.forHealth(PHASES, fraction);
        if (phase.equals(boss.phase)) {
            return;
        }
        boss.phase = phase;
        boss.bar.name(barTitle(phase));
        plugin.getLogger().info("Don Pollo Boss en " + phase.title() + " (vida " + Math.round(fraction * 100) + "%)");
        applyPhaseSpeed(ravager, phase);
        applyPhaseScale(ravager, phase);
        long now = Bukkit.getCurrentTick();
        if (boss.levitateEnd > 0 && !phase.levitate()) {
            cubes.finishLevitation(boss);
        }
        boss.bar.color(phase.has(DonPollosBossPhase.Ability.GROW) ? BossBar.Color.PURPLE : BossBar.Color.RED);
        boss.nextBeam = now + 60;
        boss.nextJump = now + boss.difficulty.cooldownTicks(settings.phase4().jumpCooldownSeconds() * 10);
        boss.nextFinalColonels = now + 60;
        if (DonPollosBossPhase.KEEP_AWAY.equals(phase.ai())) {
            ravager.setTarget(null);
        }
        boss.nextMeteor = now + 40;
        boss.nextChickens = now + 40;
        // si la fase tiene mensaje, el texto grande es el mensaje (en la FASE 3 queda mientras levita)
        boolean hasMessage = !phase.message().isEmpty();
        long stayMillis = phase.levitate() ? Math.round(settings.phase3().levitateSeconds() * 1000)
            : hasMessage ? 3000 : 1800;
        // un mensaje largo con coma va en dos renglones (titulo y subtitulo)
        String main = hasMessage ? "§4§o" + phase.message() : "§4§l" + phase.title();
        String sub = hasMessage ? "§c§l" + phase.title() : "§7Don Pollo Boss";
        int comma = phase.message().indexOf(", ");
        if (hasMessage && comma > 0 && phase.message().length() > 30) {
            main = "§4§l" + phase.message().substring(0, comma);
            sub = "§c§o" + phase.message().substring(comma + 2) + " §8| §c§l" + phase.title();
        }
        net.kyori.adventure.title.Title title = net.kyori.adventure.title.Title.title(
            LEGACY.deserialize(main),
            LEGACY.deserialize(sub),
            net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(300),
                java.time.Duration.ofMillis(stayMillis), java.time.Duration.ofMillis(700)));
        for (UUID viewer : boss.viewers) {
            Player player = Bukkit.getPlayer(viewer);
            if (player != null) {
                player.showTitle(title);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, hasMessage ? 0.6f : 1.4f);
            }
        }
        if (phase.levitate()) {
            if (boss.firingUntil > 0) {
                attacks.endLaser(boss, now);
            }
            cubes.startLevitation(boss, now);
        }
    }

    /** En la FASE FINAL se agranda; en las otras vuelve al tamano normal. */
    void applyPhaseScale(Ravager ravager, DonPollosBossPhase phase) {
        boolean grow = phase.has(DonPollosBossPhase.Ability.GROW);
        setAttribute(ravager, Attribute.SCALE, settings.scale() * (grow ? settings.phase4().growFactor() : 1));
        if (grow) {
            World world = ravager.getWorld();
            world.playSound(ravager.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 3f, 0.5f);
            world.spawnParticle(Particle.WITCH, ravager.getLocation().add(0, 1.5, 0), 80, 1.5, 1.5, 1.5, 0.02,
                null, true);
        }
    }

    /**
     * El Ravager reescribe su velocidad base en cada tick, asi que la fase no la pisa: le suma un
     * modificador para que persiguiendo vaya justo a la velocidad que corresponde.
     */
    void applyPhaseSpeed(Ravager ravager, DonPollosBossPhase phase) {
        AttributeInstance speed = ravager.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "boss_phase_speed");
        speed.removeModifier(key);
        if (DonPollosBossPhase.BABY_ZOMBIE.equals(phase.ai())) {
            speed.addModifier(new org.bukkit.attribute.AttributeModifier(key,
                BABY_ZOMBIE_SPEED / RAVAGER_CHASE_SPEED - 1,
                org.bukkit.attribute.AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                org.bukkit.inventory.EquipmentSlotGroup.ANY));
        }
    }

    void updateBar(DonPollosBoss boss) {
        Ravager ravager = boss.ravager;
        double max = ravager.getAttribute(Attribute.MAX_HEALTH).getValue();
        boss.bar.progress((float) Math.max(0, Math.min(1, ravager.getHealth() / max)));
        Set<UUID> inside = new HashSet<>();
        for (Player player : ravager.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(ravager.getLocation()) <= BAR_RADIUS * BAR_RADIUS) {
                inside.add(player.getUniqueId());
                if (boss.viewers.add(player.getUniqueId())) {
                    player.showBossBar(boss.bar);
                }
            }
        }
        for (UUID viewer : new ArrayList<>(boss.viewers)) {
            if (!inside.contains(viewer)) {
                boss.viewers.remove(viewer);
                Player player = Bukkit.getPlayer(viewer);
                if (player != null) {
                    player.hideBossBar(boss.bar);
                }
            }
        }
    }

    @EventHandler
    public void onMelee(EntityDamageByEntityEvent event) {
        if (isBoss(event.getDamager())) {
            binder.playOnce(event.getDamager().getUniqueId(), "golpe", () -> { });
            if (event.getEntity() instanceof Player player) {
                attacks.blame(player, DonPollosBossAttacks.DeathCause.RAM);
            }
        } else if (event.getEntity() instanceof Player player) {
            minions.kind(event.getDamager()).ifPresent(kind -> attacks.blame(player, switch (kind) {
                case GALLINA -> DonPollosBossAttacks.DeathCause.CHICKEN;
                case CORONEL -> DonPollosBossAttacks.DeathCause.COLONEL;
                case CORONEL_FINAL -> DonPollosBossAttacks.DeathCause.FINAL_COLONEL;
                default -> DonPollosBossAttacks.DeathCause.LABUBU;
            }));
        }
    }

    /** El boss no se lastima al caer (por ejemplo al bajar despues de levitar). */
    @EventHandler(ignoreCancelled = true)
    public void onBossFall(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FALL && isBoss(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /** Cuando se mantiene a distancia (FASE 2 y 3) el boss no elige objetivo, asi no embiste. */
    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() == null) {
            return;
        }
        DonPollosBoss boss = bosses.get(event.getEntity().getUniqueId());
        if (boss != null && boss.phase != null && DonPollosBossPhase.KEEP_AWAY.equals(boss.phase.ai())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        if (!isBoss(event.getEntity())) {
            return;
        }
        // ganarle no lo mata (ver onBossDamage): esto solo pasa con /kill o el vacio
        event.getDrops().clear();
        event.setDroppedExp(0);
        untrack(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (isBoss(entity)) {
                track((Ravager) entity);
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (isBoss(entity)) {
                untrack(entity.getUniqueId());
            }
        }
    }

    boolean removeNearest(Location location, double maxDistance) {
        DonPollosBoss nearest = null;
        double best = maxDistance * maxDistance;
        for (DonPollosBoss boss : bosses.values()) {
            if (boss.ravager.getWorld() == location.getWorld()) {
                double distance = boss.ravager.getLocation().distanceSquared(location);
                if (distance <= best) {
                    best = distance;
                    nearest = boss;
                }
            }
        }
        if (nearest == null) {
            return false;
        }
        untrack(nearest.ravager.getUniqueId());
        nearest.ravager.remove();
        return true;
    }
}
