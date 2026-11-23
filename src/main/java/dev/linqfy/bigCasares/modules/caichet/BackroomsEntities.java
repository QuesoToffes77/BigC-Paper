package dev.linqfy.bigCasares.modules.caichet;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Las entidades de los backrooms (el mundo del hotel): la Sonrisa, la Entidad 666, Juan Carlos, la Araña y el Payaso.
 * Cada una es solo de su jugador (nadie mas la ve ni la oye), a lo sumo una por jugador, y si te alcanza hay un susto y
 * morís. Este es el director: sortea cuando aparece una ({@link BackroomsRules.Director}), las hace andar cada tick
 * ({@link BackroomsEncounters}) y se ocupa del susto y el mensaje de muerte.
 */
final class BackroomsEntities implements Runnable, Listener {

    private static final Key SCARE_FONT = Key.key("caichet", "sustos");
    private static final String AMBIENT_KEY = "backrooms.ambient";
    private static final long AMBIENT_MIN_TICKS = 20 * 25;
    private static final long AMBIENT_MAX_TICKS = 20 * 45;
    private static final int SCARE_STAY_TICKS = 25;
    private static final int SCARE_FADE_OUT_TICKS = 10;

    private final Watchers watchers;
    private final BackroomsRules.Director director = new BackroomsRules.Director();
    private final BackroomsRules.PendingDeaths pending = new BackroomsRules.PendingDeaths();
    private final Map<UUID, BackroomsEncounters.Encounter> active = new HashMap<>();
    private final Map<UUID, Set<BackroomsKind>> hinted = new HashMap<>();
    private long tick;

    BackroomsEntities(Watchers watchers) {
        this.watchers = watchers;
    }

    @Override
    public void run() {
        tick++;
        World hotel = HotelWorld.get();
        for (Iterator<BackroomsEncounters.Encounter> it = active.values().iterator(); it.hasNext(); ) {
            BackroomsEncounters.Encounter encounter = it.next();
            Player player = encounter.player;
            if (hotel == null || !player.isOnline() || !player.getWorld().equals(hotel) || player.isDead()
                || player.getGameMode() == GameMode.SPECTATOR || !encounter.figure.valid()) {
                finish(encounter);
                it.remove();
                continue;
            }
            boolean done = encounter.scaring() ? scareTick(encounter) : encounter.tick();
            if (done) {
                finish(encounter);
                it.remove();
            }
        }
        if (hotel == null) {
            return;
        }
        for (Player player : hotel.getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            UUID id = player.getUniqueId();
            if (CaiChetSounds.due(id, AMBIENT_KEY, tick, AMBIENT_MIN_TICKS, AMBIENT_MAX_TICKS,
                ThreadLocalRandom.current())) {
                CaiChetSounds.playTo(player, player.getLocation(), CaiChetSounds.BACKROOMS_AMBIENT,
                    SoundCategory.AMBIENT, 0.6f, 1f);
            }
            boolean free = !active.containsKey(id) && !watchers.has(id) && !player.isDead();
            if (director.roll(id, tick, free, ThreadLocalRandom.current()) && free) {
                BackroomsKind[] kinds = BackroomsKind.values();
                spawn(player, kinds[ThreadLocalRandom.current().nextInt(kinds.length)]);
            }
        }
    }

    /**
     * Hace aparecer una entidad para este jugador (si esta en el hotel y no tiene ya otra). El comando la usa sin
     * esperar al sorteo.
     *
     * @return true si aparecio
     */
    boolean spawn(Player player, BackroomsKind kind) {
        World hotel = HotelWorld.get();
        if (hotel == null || !player.getWorld().equals(hotel) || player.isDead()) {
            return false;
        }
        BackroomsEncounters.Encounter old = active.remove(player.getUniqueId());
        if (old != null) {
            old.remove();
        }
        BackroomsEncounters.Encounter encounter = BackroomsEncounters.create(this, player, kind);
        if (encounter == null) {
            return false;
        }
        active.put(player.getUniqueId(), encounter);
        hint(player, kind);
        return true;
    }

    /** La pista de la primera vez que aparece cada tipo para este jugador (en memoria, no se guarda). */
    private void hint(Player player, BackroomsKind kind) {
        Set<BackroomsKind> seen = hinted.computeIfAbsent(player.getUniqueId(), id -> EnumSet.noneOf(BackroomsKind.class));
        if (!seen.add(kind)) {
            return;
        }
        boolean greeting = kind == BackroomsKind.JUANCARLOS;
        String text = greeting ? kind.hint.formatted(player.getName()) : kind.hint;
        actionBar(player, Component.text(text, greeting ? NamedTextColor.WHITE : NamedTextColor.GRAY));
    }

    void actionBar(Player player, Component text) {
        player.sendActionBar(text);
    }

    // ------------------------------------------------------------------ susto y muerte

    /** Te alcanzo: susto a pantalla completa y, 12 ticks despues, muerte. */
    void startScare(BackroomsEncounters.Encounter encounter) {
        if (encounter.scaring()) {
            return;
        }
        encounter.scareTicks = 0;
        Player player = encounter.player;
        pending.put(player.getUniqueId(), encounter.kind, tick);
        Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofMillis(SCARE_STAY_TICKS * 50L),
            Duration.ofMillis(SCARE_FADE_OUT_TICKS * 50L));
        if (Bedrock.is(player)) {
            // Bedrock no tiene la fuente con la imagen: un "!" rojo y oscuridad
            player.showTitle(Title.title(Component.text("!", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.empty(), times));
            player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 15, 0, false, false, false));
        } else {
            // sin sombra: la del titulo dibujaria la imagen dos veces, corrida
            Component image = Component.text(JumpscareFrames.glyphs(encounter.kind)).font(SCARE_FONT)
                .shadowColor(ShadowColor.none());
            player.showTitle(Title.title(image, Component.empty(), times));
        }
        CaiChetSounds.playTo(player, player.getLocation(), CaiChetSounds.ENTITY_JUMPSCARE, SoundCategory.HOSTILE, 3f, 1f);
        CaiChetSounds.playTo(player, player.getLocation(), CaiChetSounds.ENTITY_ATTACK, SoundCategory.HOSTILE, 2f, 1f);
        encounter.figure.setMode(BackroomsRigs.Mode.IDLE);
        encounter.figure.snapToFace(player);
    }

    /** @return true cuando termino el susto */
    private boolean scareTick(BackroomsEncounters.Encounter encounter) {
        Player player = encounter.player;
        encounter.scareTicks++;
        encounter.figure.snapToFace(player);
        encounter.figure.tick();
        if (encounter.scareTicks == BackroomsRules.SCARE_DEATH_AT && BackroomsRules.dies(player.getGameMode())) {
            player.damage(1000);
        }
        return encounter.scareTicks >= BackroomsRules.SCARE_END_AT;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        BackroomsKind kind = pending.take(player.getUniqueId(), tick);
        if (kind != null) {
            event.deathMessage(Component.text(kind.deathMessage(player.getName(), ThreadLocalRandom.current()),
                TextColor.color(kind.deathColor)));
        }
        BackroomsEncounters.Encounter encounter = active.remove(player.getUniqueId());
        if (encounter != null) {
            finish(encounter);
        }
    }

    @EventHandler
    public void onSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        BackroomsEncounters.Encounter encounter = active.get(event.getPlayer().getUniqueId());
        if (encounter != null) {
            encounter.onSwing();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        BackroomsEncounters.Encounter encounter = active.remove(id);
        if (encounter != null) {
            finish(encounter);
        }
        director.forget(id);
        pending.forget(id);
        hinted.remove(id);
    }

    /** Termino un encuentro, como termine: la figura se va y el jugador tiene 45 s sin entidades. */
    private void finish(BackroomsEncounters.Encounter encounter) {
        encounter.remove();
        director.ended(encounter.player.getUniqueId(), tick);
    }

    /** Si este jugador tiene una entidad ahora. */
    boolean has(UUID player) {
        return active.containsKey(player);
    }

    void removeAll() {
        active.values().forEach(BackroomsEncounters.Encounter::remove);
        active.clear();
        director.clear();
        pending.clear();
        hinted.clear();
    }

    /** Cuantas entidades hay vivas por tipo (para diagnostico). */
    Map<BackroomsKind, Integer> counts() {
        Map<BackroomsKind, Integer> out = new EnumMap<>(BackroomsKind.class);
        active.values().forEach(e -> out.merge(e.kind, 1, Integer::sum));
        return out;
    }
}
