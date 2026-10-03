package dev.linqfy.bigCasares.modules.glider;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.items.CustomItemRegistry;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Owns the deployed canopy shown above each actively gliding player. */
final class GliderVisualController implements AutoCloseable {

    private static final String ENTITY_TAG = "bigcasares_glider_visual";

    private final BigCasares plugin;
    private final CustomItemRegistry registry;
    private final GliderVisualSettings settings;
    private final Map<UUID, ActiveVisual> active = new HashMap<>();

    GliderVisualController(
        BigCasares plugin,
        CustomItemRegistry registry,
        GliderVisualSettings settings
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.settings = Objects.requireNonNull(settings, "settings");
        removeOrphans();
    }

    void sync(Player player, GliderTier tier) {
        if (!settings.enabled()) {
            hide(player.getUniqueId());
            return;
        }
        UUID playerId = player.getUniqueId();
        ActiveVisual current = active.get(playerId);
        if (current == null || !current.display().isValid() || current.display().isDead()) {
            hide(playerId);
            current = create(player, tier);
            active.put(playerId, current);
        } else if (current.tier() != tier) {
            current.display().setItemStack(deployedItem(tier));
            current = new ActiveVisual(current.display(), tier);
            active.put(playerId, current);
        }
        current.display().teleport(target(player));
    }

    void hide(UUID playerId) {
        ActiveVisual removed = active.remove(playerId);
        if (removed != null && removed.display().isValid()) {
            removed.display().remove();
        }
    }

    int size() {
        return active.size();
    }

    @Override
    public void close() {
        for (ActiveVisual visual : active.values()) {
            if (visual.display().isValid()) {
                visual.display().remove();
            }
        }
        active.clear();
    }

    private ActiveVisual create(Player player, GliderTier tier) {
        ItemDisplay display = player.getWorld().spawn(target(player), ItemDisplay.class, entity -> {
            entity.addScoreboardTag(ENTITY_TAG);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setGravity(false);
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setInterpolationDuration(settings.interpolationTicks());
            entity.setInterpolationDelay(0);
            entity.setTeleportDuration(settings.teleportDurationTicks());
            entity.setViewRange(settings.viewRange());
            entity.setShadowRadius(0.0F);
            entity.setShadowStrength(0.0F);
            entity.setDisplayWidth((float) (3.2 * settings.scale()));
            entity.setDisplayHeight((float) (2.2 * settings.scale()));
            float scale = (float) settings.scale();
            entity.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf()
            ));
            entity.setItemStack(deployedItem(tier));
        });
        return new ActiveVisual(display, tier);
    }

    private ItemStack deployedItem(GliderTier tier) {
        ItemStack stack = registry.createItemStack(tier.catalogId(), 1);
        ItemMeta meta = stack.getItemMeta();
        meta.setItemModel(deployedModelKey(tier));
        stack.setItemMeta(meta);
        return stack;
    }

    static NamespacedKey deployedModelKey(GliderTier tier) {
        Objects.requireNonNull(tier, "tier");
        return new NamespacedKey("bigcasares", "glider_deployed_tier_" + tier.number());
    }

    private Location target(Player player) {
        Location location = player.getLocation().add(0.0, settings.verticalOffset(), 0.0);
        location.setYaw((float) (player.getLocation().getYaw() + settings.yawOffsetDegrees()));
        location.setPitch(0.0F);
        return location;
    }

    private void removeOrphans() {
        plugin.getServer().getWorlds().forEach(world ->
            world.getEntitiesByClass(ItemDisplay.class).stream()
                .filter(display -> display.getScoreboardTags().contains(ENTITY_TAG))
                .forEach(ItemDisplay::remove));
    }

    private record ActiveVisual(ItemDisplay display, GliderTier tier) {
    }
}
