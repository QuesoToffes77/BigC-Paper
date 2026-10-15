package dev.linqfy.bigCasares.modules.donpollos;

import io.papermc.paper.entity.LookAnchor;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/** Batallas de baile contra el Don Pollo Salsero (boton PVP). */
final class DonPollosBattleManager implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final long INTRO_TICKS = 60;

    private final JavaPlugin plugin;
    private final DonPollosModelBinder binder;
    private final DonPollosBattleSettings settings;
    private final List<String> dances;
    private final Consumer<Villager> restoreBehaviour;
    private final Consumer<Villager> onWin;
    private final java.util.function.Predicate<UUID> bedrockPlayer;
    /** Batallas contra el Salsero que jugo y que gano cada jugador (guardadas en el jugador, sobreviven reinicios). */
    private final org.bukkit.NamespacedKey playedKey;
    private final org.bukkit.NamespacedKey wonKey;
    /** Cuando (epoch millis) le pago el Salsero por ultima vez a cada jugador, para el enfriamiento del premio. */
    private final org.bukkit.NamespacedKey lastRewardKey;
    /** Items del premio ganados pero no entregados porque el jugador salio antes (se entregan al volver a entrar). */
    private final org.bukkit.NamespacedKey pendingRewardKey;
    private final DonPollosBattleRegistry<Session> registry = new DonPollosBattleRegistry<>();
    private BukkitTask task;

    DonPollosBattleManager(JavaPlugin plugin, DonPollosModelBinder binder, DonPollosBattleSettings settings,
                           List<String> dances, Consumer<Villager> restoreBehaviour, Consumer<Villager> onWin,
                           java.util.function.Predicate<UUID> bedrockPlayer) {
        this.plugin = plugin;
        this.bedrockPlayer = bedrockPlayer;
        this.playedKey = new org.bukkit.NamespacedKey(plugin, "salsero_jugadas");
        this.wonKey = new org.bukkit.NamespacedKey(plugin, "salsero_ganadas");
        this.lastRewardKey = new org.bukkit.NamespacedKey(plugin, "salsero_ultimo_premio");
        this.pendingRewardKey = new org.bukkit.NamespacedKey(plugin, "salsero_premio_pendiente");
        this.binder = binder;
        this.settings = settings;
        this.dances = dances;
        this.restoreBehaviour = restoreBehaviour;
        this.onWin = onWin;
    }

    void startTicking() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    boolean isBattling(UUID playerId) {
        return registry.isBattling(playerId);
    }

    boolean isBusy(UUID salseroId) {
        return registry.isBusy(salseroId);
    }

    // ------------------------------------------------------------ inicio

    void start(Player player, Villager salsero) {
        if (isBattling(player.getUniqueId())) {
            player.sendMessage("§cYa estas en una batalla de baile.");
            return;
        }
        if (isBusy(salsero.getUniqueId())) {
            player.sendMessage("§cEste Salsero ya esta bailando contra otro.");
            return;
        }
        long now = Bukkit.getCurrentTick();
        int played = count(player, playedKey);
        int won = count(player, wonKey);
        DonPollosDanceBattle battle = new DonPollosDanceBattle(settings,
            bound -> ThreadLocalRandom.current().nextInt(bound), now, INTRO_TICKS, won);
        BossBar bar = BossBar.bossBar(text("§d§lDon Pollo Salsero §7- ¡Batalla de baile!"), 1f,
            BossBar.Color.PINK, BossBar.Overlay.NOTCHED_12);
        Session session = new Session(player.getUniqueId(), salsero, battle, bar);
        session.lastInput = player.getCurrentInput();
        if (!registry.register(session)) {
            // otro inicio se colo en el medio: no se toca el Salsero ni se cuenta la batalla
            player.sendMessage("§cNo se pudo empezar la batalla de baile, probá de nuevo.");
            return;
        }
        setCount(player, playedKey, played + 1);

        salsero.getPathfinder().stopPathfinding();
        salsero.setAI(false);
        face(salsero, player);
        player.showBossBar(bar);
        player.showTitle(Title.title(text("§d§l¡A BAILAR!"), text("§7Copia los pasos que te pide el Salsero"),
            Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2200), Duration.ofMillis(400))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
        player.sendMessage("§d¡Batalla de baile! §7Hace el paso que aparece en pantalla (W/A/S/D, saltar o agacharte). "
            + "Tenes §c" + settings.lives() + " vidas§7.");
        player.sendMessage("§dLe ganaste §f" + won + "§d de §f" + played + "§d batallas al Salsero. §7Tiempo para cada paso: §f"
            + String.format(java.util.Locale.ROOT, "%.2f", DonPollosDanceBattle.startWindowTicks(settings, won) / 20.0)
            + " s§7.");
        playNextDance(session);
    }

    // ------------------------------------------------------------ bailes encadenados

    private void playNextDance(Session session) {
        if (!session.active() || dances.isEmpty()) {
            return;
        }
        String next = dances.get(ThreadLocalRandom.current().nextInt(dances.size()));
        if (dances.size() > 1) {
            while (next.equals(session.dance)) {
                next = dances.get(ThreadLocalRandom.current().nextInt(dances.size()));
            }
        }
        String chosen = next;
        session.dance = chosen;
        UUID salseroId = session.salsero.getUniqueId();
        binder.playOnce(salseroId, chosen, () -> {
            if (session.tasksCancelled()) {
                return;
            }
            BukkitTask next2 = Bukkit.getScheduler().runTask(plugin, () -> {
                if (session.active() && chosen.equals(session.dance)) {
                    playNextDance(session);
                }
            });
            session.track(next2::cancel);
        });
    }

    private void stopDance(Session session) {
        if (session.dance != null) {
            binder.stopAnimation(session.salsero.getUniqueId(), session.dance);
            session.dance = null;
        }
    }

    // ------------------------------------------------------------ juego

    private void tick() {
        long now = Bukkit.getCurrentTick();
        for (Session session : registry.sessions()) {
            if (!session.active()) {
                continue;
            }
            Player player = Bukkit.getPlayer(session.playerId);
            if (player == null || !session.salsero.isValid()) {
                end(session, null);
                continue;
            }
            if (player.getWorld() != session.salsero.getWorld()
                || player.getLocation().distanceSquared(session.salsero.getLocation())
                    > settings.maxDistance() * settings.maxDistance()) {
                player.sendMessage("§eTe fuiste de la pista: la batalla de baile se corto.");
                end(session, player);
                continue;
            }
            if (now % 10 == 0) {
                face(session.salsero, player);
            }
            if (session.joystick != null) {
                Location at = player.getLocation();
                double dx = at.getX() - session.joystickFrom.getX();
                double dz = at.getZ() - session.joystickFrom.getZ();
                if (dx * dx + dz * dz >= DonPollosJoystick.MIN_MOVE * DonPollosJoystick.MIN_MOVE
                    || now - session.joystickAt >= DonPollosJoystick.WAIT_TICKS
                    || session.battle.ticksLeft(now) <= 1) {
                    resolveJoystick(session, player, now);
                }
            }
            handle(session, player, session.battle.tick(now), now);
            actionBar(session, player, now);
        }
    }

    @EventHandler
    public void onInput(PlayerInputEvent event) {
        Session session = registry.get(event.getPlayer().getUniqueId());
        if (session == null || !session.active()) {
            return;
        }
        Input now = event.getInput();
        Input before = session.lastInput;
        session.lastInput = now;
        Set<DonPollosDanceMove> pressed = EnumSet.noneOf(DonPollosDanceMove.class);
        if (now.isJump() && (before == null || !before.isJump())) pressed.add(DonPollosDanceMove.JUMP);
        if (now.isSneak() && (before == null || !before.isSneak())) pressed.add(DonPollosDanceMove.SNEAK);
        if (now.isLeft() && (before == null || !before.isLeft())) pressed.add(DonPollosDanceMove.LEFT);
        if (now.isRight() && (before == null || !before.isRight())) pressed.add(DonPollosDanceMove.RIGHT);
        if (now.isForward() && (before == null || !before.isForward())) pressed.add(DonPollosDanceMove.FORWARD);
        if (now.isBackward() && (before == null || !before.isBackward())) pressed.add(DonPollosDanceMove.BACKWARD);
        long tick = Bukkit.getCurrentTick();
        Player player = event.getPlayer();
        if (session.joystick != null && !pressed.isEmpty()) {
            resolveJoystick(session, player, tick);  // llego otra tecla antes de decidir la diagonal anterior
        }
        if (bedrockPlayer.test(player.getUniqueId()) && DonPollosJoystick.isDiagonal(pressed)
            && session.battle.current() != null) {
            // Bedrock con el joystick en diagonal: se decide en unos ticks, segun para donde se movio de verdad
            session.joystick = pressed;
            session.joystickFrom = player.getLocation();
            session.joystickAt = tick;
            return;
        }
        handle(session, player, session.battle.input(tick, pressed), tick);
    }

    /** Elige el paso de la diagonal del joystick segun el movimiento real y lo cuenta como respuesta. */
    private void resolveJoystick(Session session, Player player, long now) {
        Set<DonPollosDanceMove> diagonal = session.joystick;
        Location from = session.joystickFrom;
        session.joystick = null;
        session.joystickFrom = null;
        Location at = player.getLocation();
        double[] local = DonPollosJoystick.local(at.getX() - from.getX(), at.getZ() - from.getZ(), at.getYaw());
        DonPollosDanceMove move = DonPollosJoystick.dominant(diagonal, local[0], local[1]);
        handle(session, player, session.battle.input(now, move == null ? diagonal : EnumSet.of(move)), now);
    }

    private void handle(Session session, Player player, DonPollosDanceBattle.Outcome outcome, long now) {
        DonPollosDanceBattle battle = session.battle;
        switch (outcome) {
            case PROMPT -> {
                DonPollosDanceMove move = battle.current();
                long stay = Math.max(1, battle.windowTicks() - 4) * 50L;
                player.showTitle(Title.title(text(move.title()), text(move.arrows()),
                    Title.Times.times(Duration.ofMillis(60), Duration.ofMillis(stay), Duration.ofMillis(150))));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
            }
            case HIT -> {
                session.bar.progress(battle.salseroHealth());
                player.showTitle(Title.title(text("§a§l✔ ¡BIEN!"), Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(350), Duration.ofMillis(150))));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f,
                    0.8f + 1.0f * (1 - battle.salseroHealth()));
                session.salsero.getWorld().spawnParticle(Particle.NOTE,
                    session.salsero.getLocation().add(0, 2.4, 0), 4, 0.5, 0.3, 0.5, 1.0);
            }
            case MISS, TIMEOUT -> {
                player.showTitle(Title.title(text(outcome == DonPollosDanceBattle.Outcome.MISS
                        ? "§c§l✘ ¡ESE NO!" : "§c§l✘ ¡TARDE!"), text("§7Te quedan §c" + battle.lives() + " §7vidas"),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(700), Duration.ofMillis(200))));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 0.9f);
            }
            case WON -> win(session, player);
            case LOST -> lose(session, player);
            default -> {
            }
        }
    }

    private void actionBar(Session session, Player player, long now) {
        DonPollosDanceBattle battle = session.battle;
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < settings.lives(); i++) {
            bar.append(i < battle.lives() ? "§c❤" : "§8❤");
        }
        bar.append("  §7Salsero: §d").append(battle.hitsLeft()).append(" pasos");
        if (battle.current() != null) {
            int total = battle.windowTicks();
            int filled = (int) Math.ceil(10.0 * battle.ticksLeft(now) / Math.max(1, total));
            bar.append("  §e");
            for (int i = 0; i < 10; i++) {
                bar.append(i < filled ? "▮" : "§8▮");
            }
        }
        player.sendActionBar(text(bar.toString()));
    }

    // ------------------------------------------------------------ final

    private int count(Player player, org.bukkit.NamespacedKey key) {
        return player.getPersistentDataContainer().getOrDefault(key, org.bukkit.persistence.PersistentDataType.INTEGER, 0);
    }

    private void setCount(Player player, org.bukkit.NamespacedKey key, int value) {
        player.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.INTEGER, value);
    }

    private void win(Session session, Player player) {
        session.over = true;
        int won = count(player, wonKey) + 1;
        setCount(player, wonKey, won);
        long nowMillis = System.currentTimeMillis();
        long waitMinutes = settings.rewardCooldownRemainingMinutes(lastReward(player), nowMillis);
        int amount = waitMinutes > 0 ? 0 : settings.rewardAmount(won);
        player.sendMessage("§dYa le ganaste §f" + won + "§d de §f" + count(player, playedKey)
            + "§d batallas al Salsero. §7La proxima vas a tener menos tiempo...");
        if (amount > 0) {
            // se anota el pago ya, y lo que se debe queda en la sesion hasta entregarlo (o guardarlo si sale)
            player.getPersistentDataContainer().set(lastRewardKey,
                org.bukkit.persistence.PersistentDataType.LONG, nowMillis);
            session.owedReward = amount;
        } else if (waitMinutes > 0) {
            player.sendMessage("§7El Salsero ya te pago hace poco: volve en " + waitMinutes + " minutos.");
        }
        session.bar.progress(0f);
        stopDance(session);
        player.hideBossBar(session.bar);
        player.showTitle(Title.title(text("§a§l¡GANASTE!"), text("§7El Salsero se rinde..."),
            Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(2500), Duration.ofMillis(500))));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        Villager salsero = session.salsero;
        onWin.accept(salsero);
        restoreBehaviour.accept(salsero);
        salsero.setAI(true);
        salsero.getPathfinder().moveTo(player, 1.0);
        int[] waited = {0};
        BukkitTask[] walk = new BukkitTask[1];
        walk[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!registry.owns(session) || session.tasksCancelled()) {
                // la sesion ya termino o fue reemplazada: esta tarea vieja no toca nada
                walk[0].cancel();
                return;
            }
            waited[0] += 5;
            Player target = Bukkit.getPlayer(session.playerId);
            boolean close = target != null && salsero.isValid() && target.getWorld() == salsero.getWorld()
                && target.getLocation().distanceSquared(salsero.getLocation()) <= 2.5 * 2.5;
            if (target != null && salsero.isValid() && !close && waited[0] < 100) {
                salsero.getPathfinder().moveTo(target, 1.0);
                return;
            }
            walk[0].cancel();
            if (target == null) {
                // onQuit ya guardo el premio pendiente (y libero la sesion); si aun se debe algo, nunca se pierde en silencio
                end(session, Bukkit.getPlayer(session.playerId));
                if (session.owedReward > 0) {
                    plugin.getLogger().warning("Salsero: se perdio un premio pendiente de " + session.owedReward
                        + " para " + session.playerId);
                }
            } else if (salsero.isValid()) {
                salsero.getPathfinder().stopPathfinding();
                face(salsero, target);
                if (session.owedReward > 0) {
                    throwReward(session, salsero, target);  // libera la sesion al terminar de tirar
                } else {
                    release(session);
                }
            } else {
                giveOwed(session, target);
                release(session);
            }
        }, 5L, 5L);
        session.track(walk[0]::cancel);
    }

    private void throwReward(Session session, Villager salsero, Player player) {
        Material material = Material.valueOf(settings.reward().material());
        int amount = session.owedReward;
        player.sendMessage("§d¡El Salsero te tira " + amount + " " + material.name().toLowerCase().replace('_', ' ')
            + "! §7Bien bailado.");
        for (int i = 0; i < amount; i++) {
            BukkitTask throwTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!registry.owns(session) || session.tasksCancelled() || session.owedReward <= 0
                    || !player.isOnline()) {
                    return;
                }
                if (!salsero.isValid()) {
                    giveOwed(session, player);
                    return;
                }
                Location from = salsero.getEyeLocation();
                Vector toward = player.getLocation().toVector().subtract(from.toVector()).normalize().multiply(0.3);
                Item item = salsero.getWorld().dropItem(from, new ItemStack(material));
                item.setVelocity(toward.setY(0.25));
                item.setPickupDelay(10);
                session.owedReward--;
                salsero.getWorld().playSound(from, Sound.ENTITY_ITEM_PICKUP, 1f, 1.3f);
            }, i * 4L);
            session.track(throwTask::cancel);
        }
        // la sesion sigue viva hasta el ultimo tiro: asi un quit en el medio todavia guarda lo que falta
        BukkitTask finish = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!registry.owns(session) || session.tasksCancelled()) {
                return;
            }
            if (session.owedReward > 0 && player.isOnline()) {
                giveOwed(session, player);
            }
            release(session);
        }, amount * 4L + 1L);
        session.track(finish::cancel);
    }

    /** Entrega directo al inventario (lo que no entra cae al piso) todo lo que la sesion todavia debe. */
    private void giveOwed(Session session, Player player) {
        int owed = session.owedReward;
        session.owedReward = 0;
        giveItems(player, owed);
    }

    private void giveItems(Player player, int amount) {
        if (amount <= 0) {
            return;
        }
        Material material = Material.valueOf(settings.reward().material());
        int max = Math.max(1, material.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int part = Math.min(max, remaining);
            remaining -= part;
            player.getInventory().addItem(new ItemStack(material, part)).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }

    private long lastReward(Player player) {
        return player.getPersistentDataContainer().getOrDefault(lastRewardKey,
            org.bukkit.persistence.PersistentDataType.LONG, 0L);
    }

    /** Guarda en el jugador lo que el Salsero le debia, para darselo cuando vuelva a entrar. */
    private void storePendingReward(Session session, Player player) {
        if (session.owedReward <= 0) {
            return;
        }
        int total = player.getPersistentDataContainer().getOrDefault(pendingRewardKey,
            org.bukkit.persistence.PersistentDataType.INTEGER, 0) + session.owedReward;
        player.getPersistentDataContainer().set(pendingRewardKey,
            org.bukkit.persistence.PersistentDataType.INTEGER, total);
        session.owedReward = 0;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int pending = player.getPersistentDataContainer().getOrDefault(pendingRewardKey,
            org.bukkit.persistence.PersistentDataType.INTEGER, 0);
        if (pending <= 0) {
            return;
        }
        player.getPersistentDataContainer().remove(pendingRewardKey);
        giveItems(player, pending);
        player.sendMessage("§d¡El Salsero te debia §f" + pending + "§d "
            + settings.reward().material().toLowerCase().replace('_', ' ') + "! §7Bien bailado.");
    }

    private void lose(Session session, Player player) {
        session.over = true;
        player.hideBossBar(session.bar);
        player.showTitle(Title.title(text("§c§lPERDISTE"), text("§7El Salsero te bailo"),
            Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(2000), Duration.ofMillis(500))));
        player.sendMessage("§cEl Don Pollo Salsero te gano la batalla de baile.");
        release(session);
        player.setHealth(0);
    }

    private void end(Session session, Player player) {
        session.over = true;
        session.cancelTasks();
        if (player != null) {
            storePendingReward(session, player);
            player.hideBossBar(session.bar);
        }
        release(session);
    }

    /** Libera solo esta sesion (si sigue siendo la vigente) y cancela sus tareas; nunca toca una batalla nueva. */
    private void release(Session session) {
        session.cancelTasks();
        if (!registry.release(session)) {
            return;
        }
        stopDance(session);
        if (session.salsero.isValid()) {
            restoreBehaviour.accept(session.salsero);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Session session = registry.get(event.getPlayer().getUniqueId());
        if (session != null) {
            end(session, event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Session session = registry.get(event.getEntity().getUniqueId());
        if (session != null && session.active()) {
            end(session, event.getEntity());
        }
    }

    void shutdown() {
        if (task != null) {
            task.cancel();
        }
        for (Session session : registry.sessions()) {
            end(session, Bukkit.getPlayer(session.playerId));
        }
    }

    private static void face(Villager salsero, Player player) {
        Location eyes = player.getEyeLocation();
        salsero.lookAt(eyes.getX(), eyes.getY(), eyes.getZ(), LookAnchor.EYES);
    }

    private static Component text(String legacy) {
        return LEGACY.deserialize(legacy);
    }

    private static final class Session extends DonPollosBattleSession {
        private final Villager salsero;
        private final BossBar bar;
        private Input lastInput;
        /** Bedrock: diagonal del joystick que espera el movimiento real para decidir el paso. */
        private Set<DonPollosDanceMove> joystick;
        private Location joystickFrom;
        private long joystickAt;
        private String dance;

        private Session(UUID playerId, Villager salsero, DonPollosDanceBattle battle, BossBar bar) {
            super(playerId, salsero.getUniqueId(), battle);
            this.salsero = salsero;
            this.bar = bar;
        }
    }
}
