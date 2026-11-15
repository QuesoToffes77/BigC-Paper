package dev.linqfy.bigCasares.modules.mythicdungeons.boss;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Lo que ven los jugadores de Bedrock en lugar de un jefe o una pieza con modelo de BetterModel. Bedrock (por Geyser)
 * no dibuja los modelos de BetterModel, asi que sin esto veria un Warden invisible: nada. Cada jefe (o pieza) puede
 * tener un "doble" vanilla que solo ven los de Bedrock (a los de Java se les oculta con {@link Player#hideEntity}),
 * sin IA ni colision, que sigue al ancla cada tick del encuentro y le pasa los golpes que recibe.
 *
 * <p>Limitacion: el doble es un mob vanilla parecido (no el modelo); las animaciones de Java no existen en Bedrock.</p>
 */
public final class BedrockStandIns implements Listener {

    private final JavaPlugin plugin;
    private final Predicate<UUID> bedrockPlayer;
    /** doble -> ancla que recibe los golpes */
    private final Map<UUID, Entity> anchors = new HashMap<>();
    private final Map<UUID, LivingEntity> standIns = new HashMap<>();

    public BedrockStandIns(JavaPlugin plugin, Predicate<UUID> bedrockPlayer) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.bedrockPlayer = Objects.requireNonNull(bedrockPlayer, "bedrockPlayer");
    }

    /** Registra un doble ya creado (por ejemplo con {@link MythicBossSupport#spawnOwned}) para el ancla dada. */
    public void bind(LivingEntity standIn, Entity anchor) {
        standIn.setAI(false);
        standIn.setSilent(true);
        standIn.setCollidable(false);
        standIn.setPersistent(false);
        standIn.setRemoveWhenFarAway(false);
        standIn.setGravity(false);
        anchors.put(standIn.getUniqueId(), anchor);
        standIns.put(standIn.getUniqueId(), standIn);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            applyVisibility(player, standIn);
        }
    }

    /** Escala visual del doble (atributo SCALE), si la entidad la soporta. */
    public static void scale(LivingEntity standIn, double factor) {
        AttributeInstance scale = standIn.getAttribute(Attribute.SCALE);
        if (scale != null) {
            scale.setBaseValue(factor);
        }
    }

    /** Lleva cada doble a la posicion de su ancla; lo llama el modulo cada tick de los jefes. */
    public void tick() {
        anchors.entrySet().removeIf(entry -> {
            LivingEntity standIn = standIns.get(entry.getKey());
            Entity anchor = entry.getValue();
            if (standIn == null || !standIn.isValid() || !anchor.isValid()) {
                if (standIn != null && standIn.isValid()) {
                    standIn.remove();
                }
                standIns.remove(entry.getKey());
                return true;
            }
            Location to = anchor.getLocation();
            if (!to.getWorld().equals(standIn.getWorld()) || to.distanceSquared(standIn.getLocation()) > 0.0025) {
                standIn.teleport(to);
            }
            return false;
        });
    }

    public void clear() {
        standIns.values().stream().filter(Entity::isValid).forEach(Entity::remove);
        standIns.clear();
        anchors.clear();
    }

    int size() {
        return standIns.size();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        standIns.values().forEach(standIn -> applyVisibility(event.getPlayer(), standIn));
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity anchor = anchors.get(event.getEntity().getUniqueId());
        if (anchor == null) {
            return;
        }
        event.setCancelled(true);
        if (!(anchor instanceof LivingEntity living) || !anchor.isValid()) {
            return;
        }
        Entity source = event instanceof EntityDamageByEntityEvent byEntity ? byEntity.getDamager() : null;
        // va por el EntityDamageEvent del ancla, asi pveboss aplica filterDamage y el ranking igual que en Java. Un
        // proyectil se pasa como proyectil (no como golpe del tirador): el encuentro decide si lo bloquea.
        if (source != null) {
            living.damage(event.getDamage(), source);
        } else {
            living.damage(event.getDamage());
        }
        if (source instanceof Projectile projectile && projectile.isValid()) {
            projectile.remove();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTarget(EntityTargetEvent event) {
        if (standIns.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private void applyVisibility(Player player, LivingEntity standIn) {
        if (bedrockPlayer.test(player.getUniqueId())) {
            player.showEntity(plugin, standIn);
        } else {
            player.hideEntity(plugin, standIn);
        }
    }
}
