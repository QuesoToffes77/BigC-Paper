package dev.linqfy.bigCasares.modules.airdrop;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

/**
 * Floating TextDisplay above a landed AirDrop chest. It shows the five-minute
 * lock first, then the remaining 40-minute lifetime, updating once per second.
 * The name tag and
 * its update task exist only while the chest exists and are removed by
 * {@link #stop()}, so no permanent entities or tasks are left behind.
 */
final class AirdropChestCountdown {

    private static final long UPDATE_TICKS = 20L;

    private final JavaPlugin plugin;
    private final World world;
    private final AirdropPosition position;
    private final AirdropType type;
    private final AirdropQuality quality;
    private final long unlockAtMillis;
    private final long despawnAtMillis;
    private TextDisplay nameTag;
    private BukkitTask task;
    private boolean active;

    AirdropChestCountdown(
        JavaPlugin plugin,
        World world,
        AirdropPosition position,
        AirdropType type,
        AirdropQuality quality,
        long unlockAtMillis,
        long despawnAtMillis
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.world = Objects.requireNonNull(world, "world");
        this.position = Objects.requireNonNull(position, "position");
        this.type = Objects.requireNonNull(type, "type");
        this.quality = Objects.requireNonNull(quality, "quality");
        this.unlockAtMillis = unlockAtMillis;
        this.despawnAtMillis = despawnAtMillis;
    }

    /** Spawns the visible name above the chest and starts the one-second updater. */
    void start() {
        Location above = new Location(world, position.x() + 0.5, position.y() + 1.45, position.z() + 0.5);
        nameTag = world.spawn(above, TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            entity.setSeeThrough(true);
            entity.setShadowed(true);
            entity.setDefaultBackground(false);
            entity.setLineWidth(320);
            entity.setViewRange(64.0f);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
        });
        updateText();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, UPDATE_TICKS, UPDATE_TICKS);
        active = true;
    }

    /** Cancels the updater and removes the display; idempotent. */
    void stop() {
        active = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (nameTag != null && nameTag.isValid()) {
            nameTag.remove();
        }
        nameTag = null;
    }

    boolean isActive() {
        return active;
    }

    private void tick() {
        if (!active || nameTag == null || !nameTag.isValid()) {
            stop();
            return;
        }
        if (System.currentTimeMillis() >= despawnAtMillis) {
            // The despawn callback removes the chest; only the display is left.
            stop();
            return;
        }
        updateText();
    }

    private void updateText() {
        long now = System.currentTimeMillis();
        nameTag.setText(format(type, quality, now, unlockAtMillis, despawnAtMillis));
    }

    static String format(AirdropType type, long nowMillis, long unlockAtMillis, long despawnAtMillis) {
        return format(type, null, nowMillis, unlockAtMillis, despawnAtMillis);
    }

    static String format(
        AirdropType type,
        AirdropQuality quality,
        long nowMillis,
        long unlockAtMillis,
        long despawnAtMillis
    ) {
        String name = type == null ? "Airdrop" : type.color() + type.displayName();
        String qualityName = quality == null ? "" : " §7[" + quality.color() + quality.displayName() + "§7]";
        if (nowMillis < unlockAtMillis) {
            long totalSeconds = Math.max(0L, (unlockAtMillis - nowMillis + 999L) / 1000L);
            return "§6✈ " + name + qualityName + "\n§c🔒 Se desbloquea en "
                + pad(totalSeconds / 60L) + ":" + pad(totalSeconds % 60L);
        }
        long totalSeconds = Math.max(0L, (despawnAtMillis - nowMillis + 999L) / 1000L);
        return "§6✈ " + name + qualityName + "\n§a✔ Disponible §7| Desaparece en "
            + pad(totalSeconds / 60L) + ":" + pad(totalSeconds % 60L);
    }

    private static String pad(long value) {
        return value < 10 ? "0" + value : Long.toString(value);
    }
}
