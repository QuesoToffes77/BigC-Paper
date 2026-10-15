package dev.linqfy.bigCasares.modules.donpollos;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Los listeners del cubo con eventos reales de Bukkit, un mundo y un jugador falsos (sin servidor). */
class DonPollosBossTerrainTest {

    /** Cubo en el chunk (2, -1): x 32..47, z -16..-1, de y 64 (fondo) a 70 (el suelo original). */
    private static final int CHUNK_X = 2;
    private static final int CHUNK_Z = -1;

    private final World world = fake(World.class, Map.of("getName", "world"));
    private final Map<UUID, DonPollosBoss> bosses = new HashMap<>();
    private final List<Player> escapeWarnings = new ArrayList<>();
    private final List<Player> buildWarnings = new ArrayList<>();
    private final DonPollosBossTerrain terrain = new DonPollosBossTerrain(bosses, escapeWarnings::add, buildWarnings::add);
    private DonPollosBoss boss;

    private GameMode gameMode = GameMode.SURVIVAL;
    private Location playerAt;
    private Boolean gliding;
    private Vector velocity = new Vector(0, 1.5, 0);
    private final Player player = fakePlayer();

    @BeforeEach
    void cubeBattleInProgress() {
        boss = new DonPollosBoss(null);
        boss.cube = new DonPollosCubeBattle(List.of(new DonPollosCubeRound(1, 1.0, 1.0, 0)), 5, 20, 0);
        boss.pits.add(new DonPollosBossPit(world, CHUNK_X, CHUNK_Z, 70, 64, List.of()));
        bosses.put(UUID.randomUUID(), boss);
        playerAt = inside();
    }

    private Location inside() {
        return new Location(world, 40.5, 66, -8.5);
    }

    @Test
    void ender_pearls_chorus_and_commands_cannot_take_you_out() {
        Location outside = new Location(world, 500, 80, 500);
        for (PlayerTeleportEvent.TeleportCause cause : List.of(PlayerTeleportEvent.TeleportCause.ENDER_PEARL,
            PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT, PlayerTeleportEvent.TeleportCause.COMMAND)) {
            PlayerTeleportEvent event = new PlayerTeleportEvent(player, inside(), outside, cause);
            terrain.onCubeTeleport(event);
            assertTrue(event.isCancelled(), cause.name());
        }
        assertEquals(3, escapeWarnings.size());
    }

    @Test
    void teleporting_inside_the_same_cube_is_fine() {
        PlayerTeleportEvent event = new PlayerTeleportEvent(player, inside(), new Location(world, 33, 64, -15),
            PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        terrain.onCubeTeleport(event);
        assertFalse(event.isCancelled());
        assertTrue(escapeWarnings.isEmpty());
    }

    @Test
    void creative_players_and_finished_battles_are_not_held() {
        Location outside = new Location(world, 500, 80, 500);
        gameMode = GameMode.CREATIVE;
        PlayerTeleportEvent creative = new PlayerTeleportEvent(player, inside(), outside);
        terrain.onCubeTeleport(creative);
        assertFalse(creative.isCancelled());

        gameMode = GameMode.SURVIVAL;
        boss.cube = null;
        PlayerTeleportEvent afterBattle = new PlayerTeleportEvent(player, inside(), outside);
        terrain.onCubeTeleport(afterBattle);
        assertFalse(afterBattle.isCancelled());
    }

    @Test
    void flying_out_the_top_puts_you_back_and_closes_the_elytra() {
        Location from = new Location(world, 40.5, 70.6, -8.5, 10f, 0f);
        Location to = new Location(world, 40.5, 71.4, -8.5, 45f, -20f);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        terrain.onCubeMove(event);

        assertEquals(from.getY(), event.getTo().getY());
        assertEquals(45f, event.getTo().getYaw(), "mantiene hacia donde mira");
        assertEquals(Boolean.FALSE, gliding);
        assertTrue(velocity.getY() <= 0, "sin impulso para arriba");
        assertEquals(1, escapeWarnings.size());
    }

    @Test
    void walking_around_inside_is_not_touched() {
        Location from = new Location(world, 40.5, 66, -8.5);
        Location to = new Location(world, 41.5, 66, -8.5);
        PlayerMoveEvent event = new PlayerMoveEvent(player, from, to);

        terrain.onCubeMove(event);

        assertEquals(to, event.getTo());
        assertTrue(escapeWarnings.isEmpty());
    }

    @Test
    void elytra_cannot_be_opened_inside() {
        EntityToggleGlideEvent open = new EntityToggleGlideEvent(player, true);
        terrain.onCubeGlide(open);
        assertTrue(open.isCancelled());

        playerAt = new Location(world, 500, 80, 500);
        EntityToggleGlideEvent outside = new EntityToggleGlideEvent(player, true);
        terrain.onCubeGlide(outside);
        assertFalse(outside.isCancelled());
    }

    @Test
    void pearls_and_wind_charges_cannot_be_thrown_inside() {
        ProjectileLaunchEvent pearl = new ProjectileLaunchEvent(projectile(EntityType.ENDER_PEARL));
        terrain.onCubeProjectile(pearl);
        assertTrue(pearl.isCancelled());

        ProjectileLaunchEvent windCharge = new ProjectileLaunchEvent(projectile(EntityType.WIND_CHARGE));
        terrain.onCubeProjectile(windCharge);
        assertTrue(windCharge.isCancelled());

        ProjectileLaunchEvent arrow = new ProjectileLaunchEvent(projectile(EntityType.ARROW));
        terrain.onCubeProjectile(arrow);
        assertFalse(arrow.isCancelled());
    }

    // ------------------------------------------------------------ falsos

    private Player fakePlayer() {
        UUID id = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getGameMode" -> gameMode;
                case "isDead" -> false;
                case "getLocation" -> playerAt.clone();
                case "getWorld" -> world;
                case "getUniqueId" -> id;
                case "setGliding" -> {
                    gliding = (Boolean) args[0];
                    yield null;
                }
                case "getVelocity" -> velocity.clone();
                case "setVelocity" -> {
                    velocity = (Vector) args[0];
                    yield null;
                }
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "FakePlayer";
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private Projectile projectile(EntityType type) {
        return (Projectile) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Projectile.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getShooter" -> player;
                case "getType" -> type;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "FakeProjectile(" + type + ")";
                default -> throw new UnsupportedOperationException(method.getName());
            });
    }

    private static <T> T fake(Class<T> type, Map<String, Object> answers) {
        return type.cast(Proxy.newProxyInstance(DonPollosBossTerrainTest.class.getClassLoader(), new Class<?>[]{type},
            (proxy, method, args) -> switch (method.getName()) {
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "Fake" + type.getSimpleName();
                default -> {
                    if (answers.containsKey(method.getName())) {
                        yield answers.get(method.getName());
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
            }));
    }
}
