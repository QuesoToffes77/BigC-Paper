package dev.linqfy.bigCasares.modules.nexus;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class NexusDamageResolver {

    public NexusDamageRequest resolve(NexusId nexusId, EntityDamageEvent event) {
        Objects.requireNonNull(event, "event");
        DamageSource source = event.getDamageSource();
        Entity causingEntity = source.getCausingEntity();
        Entity directEntity = source.getDirectEntity();

        return resolve(
                nexusId,
                event.getFinalDamage(),
                entityId(causingEntity),
                projectileOwnerId(directEntity),
                tntOwnerId(directEntity),
                entityId(directEntity),
                weaponId(causingEntity, directEntity),
                Optional.of(source.getDamageType().getKey().toString()),
                directEntity == null
                        ? Optional.empty()
                        : Optional.of(directEntity.getType().getKey().toString()),
                Instant.now(),
                attackerOrigin(causingEntity, directEntity)
        );
    }

    public NexusDamageRequest resolve(
            NexusId nexusId,
            double finalDamage,
            Optional<UUID> causingEntityId,
            Optional<UUID> projectileOwnerId,
            Optional<UUID> tntOwnerId,
            Optional<UUID> directEntityId,
            Optional<String> weaponId,
            Optional<String> damageTypeId,
            Optional<String> directEntityType,
            Instant occurredAt
    ) {
        return resolve(
            nexusId,
            finalDamage,
            causingEntityId,
            projectileOwnerId,
            tntOwnerId,
            directEntityId,
            weaponId,
            damageTypeId,
            directEntityType,
            occurredAt,
            resolveAttacker(causingEntityId, projectileOwnerId, tntOwnerId, directEntityId).isPresent()
                ? NexusAttackerOrigin.UNKNOWN
                : NexusAttackerOrigin.UNOWNED
        );
    }

    public NexusDamageRequest resolve(
            NexusId nexusId,
            double finalDamage,
            Optional<UUID> causingEntityId,
            Optional<UUID> projectileOwnerId,
            Optional<UUID> tntOwnerId,
            Optional<UUID> directEntityId,
            Optional<String> weaponId,
            Optional<String> damageTypeId,
            Optional<String> directEntityType,
            Instant occurredAt,
            NexusAttackerOrigin attackerOrigin
    ) {
        return new NexusDamageRequest(
                nexusId,
                resolveAttacker(causingEntityId, projectileOwnerId, tntOwnerId, directEntityId),
                categorize(damageTypeId, directEntityType),
                finalDamage,
                weaponId,
                damageTypeId,
                directEntityType,
                occurredAt,
                attackerOrigin
        );
    }

    public Optional<UUID> resolveAttacker(
            Optional<UUID> causingEntityId,
            Optional<UUID> projectileOwnerId,
            Optional<UUID> tntOwnerId,
            Optional<UUID> directEntityId
    ) {
        Objects.requireNonNull(causingEntityId, "causingEntityId");
        Objects.requireNonNull(projectileOwnerId, "projectileOwnerId");
        Objects.requireNonNull(tntOwnerId, "tntOwnerId");
        Objects.requireNonNull(directEntityId, "directEntityId");
        return causingEntityId
                .or(() -> projectileOwnerId)
                .or(() -> tntOwnerId)
                .or(() -> directEntityId);
    }

    public Optional<UUID> resolveAttacker(Entity causingEntity, Entity directEntity) {
        return resolveAttacker(
                entityId(causingEntity),
                projectileOwnerId(directEntity),
                tntOwnerId(directEntity),
                entityId(directEntity)
        );
    }

    public Optional<UUID> projectileOwnerId(Entity entity) {
        if (!(entity instanceof Projectile projectile)) {
            return Optional.empty();
        }
        ProjectileSource shooter = projectile.getShooter();
        if (shooter instanceof Entity shooterEntity) {
            return Optional.of(shooterEntity.getUniqueId());
        }
        return Optional.ofNullable(projectile.getOwnerUniqueId());
    }

    public Optional<UUID> tntOwnerId(Entity entity) {
        if (!(entity instanceof TNTPrimed tnt)) {
            return Optional.empty();
        }
        return entityId(tnt.getSource());
    }

    public NexusAttackerOrigin attackerOrigin(Entity causingEntity, Entity directEntity) {
        Entity owner = causingEntity;
        if (owner instanceof Projectile || owner instanceof TNTPrimed) {
            owner = null;
        }
        if (owner == null && directEntity instanceof Projectile projectile
            && projectile.getShooter() instanceof Entity shooter) {
            owner = shooter;
        }
        if (owner == null && directEntity instanceof TNTPrimed tnt) {
            owner = tnt.getSource();
        }
        if (owner == null && directEntity instanceof LivingEntity) {
            owner = directEntity;
        }
        if (owner instanceof Player) {
            return NexusAttackerOrigin.PLAYER;
        }
        if (owner instanceof LivingEntity) {
            return NexusAttackerOrigin.NATURAL_ENTITY;
        }
        return NexusAttackerOrigin.UNOWNED;
    }

    public NexusDamageKind categorize(
            Optional<String> damageTypeId,
            Optional<String> directEntityType
    ) {
        Objects.requireNonNull(damageTypeId, "damageTypeId");
        Objects.requireNonNull(directEntityType, "directEntityType");
        String damage = normalize(damageTypeId.orElse(""));
        String direct = normalize(directEntityType.orElse(""));

        if (containsAny(damage, "explosion", "bad_respawn_point")
                || containsAny(direct, "tnt", "creeper", "end_crystal")) {
            return NexusDamageKind.EXPLOSION;
        }
        if (containsAny(damage, "arrow", "trident", "projectile", "thrown", "wind_charge")
                || containsAny(direct, "arrow", "trident", "snowball", "egg", "firework", "wind_charge", "projectile")) {
            return NexusDamageKind.PROJECTILE;
        }
        if (containsAny(damage, "in_fire", "on_fire", "lava", "campfire", "hot_floor", "fireball")
                || containsAny(direct, "fireball", "small_fireball", "dragon_fireball")) {
            return NexusDamageKind.FIRE;
        }
        if (containsAny(damage, "magic", "indirect_magic", "sonic_boom", "wither")) {
            return NexusDamageKind.MAGIC;
        }
        if (containsAny(damage, "player_attack", "mob_attack", "mob_attack_no_aggro", "sting")
                || direct.equals("player")) {
            return NexusDamageKind.MELEE;
        }
        if (damage.equals("custom")) {
            return NexusDamageKind.CUSTOM;
        }
        if (containsAny(
                damage,
                "fall",
                "falling_block",
                "drown",
                "freeze",
                "starve",
                "generic",
                "outside_border",
                "out_of_world",
                "in_wall",
                "cramming",
                "lightning",
                "stalagmite",
                "stalactite",
                "fly_into_wall",
                "sweet_berry_bush",
                "cactus"
        )) {
            return NexusDamageKind.ENVIRONMENT;
        }
        return NexusDamageKind.UNKNOWN;
    }

    private static String normalize(String value) {
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        return separator >= 0 ? normalized.substring(separator + 1) : normalized;
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.equals(candidate) || value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<UUID> entityId(Entity entity) {
        return entity == null ? Optional.empty() : Optional.of(entity.getUniqueId());
    }

    private Optional<String> weaponId(Entity causingEntity, Entity directEntity) {
        Entity attacker = causingEntity;
        if (attacker == null && directEntity instanceof Projectile projectile
                && projectile.getShooter() instanceof Entity shooter) {
            attacker = shooter;
        }
        if (!(attacker instanceof LivingEntity livingEntity)) {
            return Optional.empty();
        }
        EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment == null) {
            return Optional.empty();
        }
        ItemStack item = equipment.getItemInMainHand();
        if (item.getType().isAir()) {
            return Optional.empty();
        }
        return Optional.of(item.getType().getKey().toString());
    }
}
