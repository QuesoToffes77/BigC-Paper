package dev.linqfy.bigCasares.modules.nexus;

import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NexusDamageResolverTest {

    private final NexusDamageResolver resolver = new NexusDamageResolver();
    private final NexusId nexusId = NexusId.random();

    @Test
    void preservesTheRealFinalDamageProducedByMinecraft() {
        NexusDamageRequest request = resolver.resolve(
                nexusId,
                13.75,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("minecraft:diamond_sword"),
                Optional.of("minecraft:player_attack"),
                Optional.of("minecraft:player"),
                Instant.parse("2026-07-11T00:00:00Z")
        );

        assertEquals(13.75, request.actualDamage());
        assertEquals(NexusDamageKind.MELEE, request.kind());
        assertEquals(Optional.of("minecraft:player"), request.directEntityType());
    }

    @Test
    void preservesCriticalDamageInsteadOfRecalculatingWeaponDamage() {
        NexusDamageRequest request = resolver.resolve(
                nexusId,
                18.625,
                Optional.of(UUID.randomUUID()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("minecraft:netherite_axe"),
                Optional.of("minecraft:player_attack"),
                Optional.of("minecraft:player"),
                Instant.now()
        );

        assertEquals(18.625, request.actualDamage());
    }

    @Test
    void paperAdapterReadsEntityDamageEventFinalDamageVerbatim() {
        UUID attackerId = UUID.randomUUID();
        Entity attacker = proxy(Entity.class, Map.of(
                "getUniqueId", attackerId,
                "getType", EntityType.PLAYER
        ));
        DamageType damageType = proxy(DamageType.class, Map.of(
                "getKey", NamespacedKey.minecraft("player_attack")
        ));
        DamageSource source = proxy(DamageSource.class, Map.of(
                "getDamageType", damageType,
                "getCausingEntity", attacker,
                "getDirectEntity", attacker
        ));
        EntityDamageEvent event = new EntityDamageEvent(
                attacker,
                EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                source,
                19.375
        );

        NexusDamageRequest request = resolver.resolve(nexusId, event);

        assertEquals(event.getFinalDamage(), request.actualDamage());
        assertEquals(19.375, request.actualDamage());
        assertEquals(Optional.of(attackerId), request.attackerId());
    }

    @Test
    void attributesProjectileDamageToItsShooterWhenDamageSourceHasNoCausingEntity() {
        UUID shooter = UUID.randomUUID();

        NexusDamageRequest request = resolver.resolve(
                nexusId,
                7.0,
                Optional.empty(),
                Optional.of(shooter),
                Optional.empty(),
                Optional.of(UUID.randomUUID()),
                Optional.of("minecraft:bow"),
                Optional.of("minecraft:arrow"),
                Optional.of("minecraft:arrow"),
                Instant.now()
        );

        assertEquals(Optional.of(shooter), request.attackerId());
        assertEquals(NexusDamageKind.PROJECTILE, request.kind());
    }

    @Test
    void extractsProjectileOwnerFromPaperProjectile() {
        UUID shooterId = UUID.randomUUID();
        EntityProjectileSource shooter = proxy(EntityProjectileSource.class, Map.of("getUniqueId", shooterId));
        Projectile projectile = proxy(Projectile.class, Map.of("getShooter", shooter));

        assertEquals(Optional.of(shooterId), resolver.projectileOwnerId(projectile));
    }

    @Test
    void causingEntityTakesPrecedenceOverProjectileAndDirectEntity() {
        UUID causingEntity = UUID.randomUUID();

        Optional<UUID> attacker = resolver.resolveAttacker(
                Optional.of(causingEntity),
                Optional.of(UUID.randomUUID()),
                Optional.of(UUID.randomUUID()),
                Optional.of(UUID.randomUUID())
        );

        assertEquals(Optional.of(causingEntity), attacker);
    }

    @Test
    void categorizesExplosionAndEnvironmentDamageTypes() {
        assertEquals(
                NexusDamageKind.EXPLOSION,
                resolver.categorize(Optional.of("minecraft:player_explosion"), Optional.of("minecraft:tnt"))
        );
        assertEquals(
                NexusDamageKind.ENVIRONMENT,
                resolver.categorize(Optional.of("minecraft:fall"), Optional.empty())
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Map<String, Object> results) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, arguments) -> {
                    if (results.containsKey(method.getName())) {
                        return results.get(method.getName());
                    }
                    return switch (method.getName()) {
                        case "toString" -> type.getSimpleName() + "Proxy";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == arguments[0];
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        return 0.0d;
    }

    private interface EntityProjectileSource extends Entity, ProjectileSource {
    }
}
