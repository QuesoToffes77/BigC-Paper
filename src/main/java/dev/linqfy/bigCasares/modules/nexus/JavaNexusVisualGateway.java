package dev.linqfy.bigCasares.modules.nexus;

import dev.linqfy.bigCasares.modules.model.JavaModelGateway;
import dev.linqfy.bigCasares.modules.model.JavaModelHandle;
import dev.linqfy.bigCasares.modules.model.JavaModelKeys;
import dev.linqfy.bigCasares.platform.ClientEntityPresentationRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class JavaNexusVisualGateway implements NexusVisualGateway {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static final String ROLE_ANCHOR = "anchor";
    private static final String ROLE_HEALTH = "health";

    private final JavaPlugin plugin;
    private final JavaModelGateway models;
    private final NexusModelAnimationPolicy animationPolicy = new NexusModelAnimationPolicy();
    private final NamespacedKey nexusIdKey;
    private final NamespacedKey visualRoleKey;
    private final Map<NexusId, VisualState> visuals = new LinkedHashMap<>();
    private final NexusVisualRecoveryPlanner recoveryPlanner = new NexusVisualRecoveryPlanner(1);
    private org.bukkit.scheduler.BukkitTask particleTask;
    private double particleAngle = 0.0;

    public JavaNexusVisualGateway(JavaPlugin plugin, JavaModelGateway models) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.models = Objects.requireNonNull(models, "models");
        this.nexusIdKey = new NamespacedKey(plugin, "nexus_id");
        this.visualRoleKey = new NamespacedKey(plugin, "nexus_visual_role");
        this.particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::spawnAuraParticles, 2L, 2L);
    }

    @Override
    public synchronized NexusVisualHandle spawn(NexusVisualRequest request) {
        Objects.requireNonNull(request, "request");
        remove(request.nexusId());

        World world = Bukkit.getWorld(request.position().worldId());
        if (world == null) {
            throw new IllegalStateException("Nexus world is not loaded: " + request.position().worldId());
        }

        Location anchorLocation = location(world, request.position());
        Ghast anchor = world.spawn(anchorLocation, Ghast.class, ghast -> configureAnchor(ghast, request.nexusId()));

        anchorLocation.getWorld().spawnParticle(Particle.FIREWORK, anchorLocation.clone().add(0, 1, 0), 50, 1.0, 1.0, 1.0, 0.1);
        anchorLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, anchorLocation.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);

        JavaModelHandle model;
        try {
            model = models.attach(anchor, JavaModelKeys.NEXUS);
            models.animate(model, animationPolicy.steadyAnimation(healthFraction(request)));
        } catch (RuntimeException failure) {
            removeEntity(anchor.getUniqueId());
            throw failure;
        }

        Location textLocation = anchorLocation.clone().add(0.0, 4.1, 0.0);
        TextDisplay health = world.spawn(textLocation, TextDisplay.class, display -> {
            configureChild(display, request.nexusId(), ROLE_HEALTH);
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowed(true);
            display.setSeeThrough(false);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setLineWidth(240);
            display.text(LEGACY.deserialize(
                renderHealth(request.teamName(), request.currentHealth(), request.maximumHealth(), false)));
        });

        NexusVisualHandle handle = new NexusVisualHandle(
                request.nexusId(),
                anchor.getUniqueId(),
                Set.of(health.getUniqueId())
        );
        visuals.put(request.nexusId(), new VisualState(request, handle, model));
        return handle;
    }

    @Override
    public synchronized void updateHealth(NexusId nexusId, double healthFraction) {
        VisualState state = visuals.get(nexusId);
        if (state == null) {
            return;
        }
        double fraction = Math.max(0.0, Math.min(1.0, healthFraction));
        state.currentHealth = state.request.maximumHealth() * fraction;
        models.animate(state.model, animationPolicy.steadyAnimation(fraction));
        entityWithRole(state.handle, ROLE_HEALTH, TextDisplay.class).ifPresent(display ->
                display.text(LEGACY.deserialize(renderHealth(
                        state.request.teamName(),
                        state.currentHealth,
                        state.request.maximumHealth(), state.underAttack
                )))
        );
    }

    public synchronized void setUnderAttack(NexusId nexusId, boolean underAttack) {
        VisualState state = visuals.get(nexusId);
        if (state == null || state.underAttack == underAttack) return;
        state.underAttack = underAttack;
        refreshText(state);
    }

    public synchronized void updateTeamName(NexusId nexusId, String teamName) {
        VisualState state = visuals.get(nexusId);
        if (state == null || state.teamName.equals(teamName)) return;
        state.teamName = teamName;
        refreshText(state);
    }

    private void refreshText(VisualState state) {
        entityWithRole(state.handle, ROLE_HEALTH, TextDisplay.class).ifPresent(display -> display.text(LEGACY.deserialize(
                renderHealth(state.teamName, state.currentHealth, state.request.maximumHealth(), state.underAttack))));
    }

    @Override
    public synchronized void playDamageAnimation(NexusId nexusId, NexusDamageKind damageKind) {
        VisualState state = visuals.get(nexusId);
        if (state != null) {
            models.animate(state.model, animationPolicy.damageAnimation());
        }
        anchor(nexusId).ifPresent(anchor -> {
            Location location = anchor.getLocation().add(0.0, 2.0, 0.0);
            anchor.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, location, 12, 1.0, 1.0, 1.0, 0.1);
            anchor.getWorld().playSound(
                    location,
                    Sound.BLOCK_BEACON_POWER_SELECT,
                    SoundCategory.HOSTILE,
                    1.0f,
                    1.8f
            );
        });
    }

    @Override
    public synchronized void playDestroyedAnimation(NexusId nexusId) {
        VisualState state = visuals.get(nexusId);
        if (state != null) {
            models.animate(state.model, animationPolicy.destroyedAnimation());
        }
        anchor(nexusId).ifPresent(anchor -> {
            Location location = anchor.getLocation().add(0.0, 2.0, 0.0);
            anchor.getWorld().spawnParticle(Particle.EXPLOSION, location, 8, 1.5, 1.5, 1.5, 0.2);
            anchor.getWorld().playSound(
                    location,
                    Sound.BLOCK_BEACON_DEACTIVATE,
                    SoundCategory.HOSTILE,
                    1.0f,
                    1.0f
            );
        });
    }

    @Override
    public synchronized void remove(NexusId nexusId) {
        VisualState state = visuals.remove(nexusId);
        if (state == null) {
            removeDiscoveredEntities(nexusId);
            return;
        }
        models.close(state.model);
        state.handle.childEntityIds().forEach(this::removeEntity);
        removeEntity(state.handle.anchorEntityId());
    }

    public synchronized Optional<NexusId> findNexusId(Entity entity) {
        if (entity == null || !ROLE_ANCHOR.equals(role(entity))) {
            return Optional.empty();
        }
        return readNexusId(entity);
    }

    public synchronized Collection<NexusVisualHandle> discoverVisualHandles() {
        Map<NexusId, UUID> anchors = new LinkedHashMap<>();
        Map<NexusId, Set<UUID>> children = new LinkedHashMap<>();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                Optional<NexusId> nexusId = readNexusId(entity);
                if (nexusId.isEmpty()) {
                    continue;
                }
                String role = role(entity);
                if (ROLE_ANCHOR.equals(role)) {
                    anchors.put(nexusId.orElseThrow(), entity.getUniqueId());
                    ClientEntityPresentationRegistry.register(entity.getUniqueId(), "GHAST", "nexus_id");
                } else {
                    children.computeIfAbsent(nexusId.orElseThrow(), ignored -> new LinkedHashSet<>())
                            .add(entity.getUniqueId());
                }
            }
        }
        List<NexusVisualHandle> handles = new ArrayList<>();
        anchors.forEach((id, anchorId) -> handles.add(new NexusVisualHandle(
                id,
                anchorId,
                children.getOrDefault(id, Set.of())
        )));
        return List.copyOf(handles);
    }

    public synchronized NexusVisualRecoveryPlan recover(Collection<NexusVisualRequest> desiredVisuals) {
        Collection<NexusVisualHandle> discovered = discoverVisualHandles();
        NexusVisualRecoveryPlan plan = recoveryPlanner.plan(desiredVisuals, discovered);
        plan.toRemove().forEach(this::remove);
        plan.toSpawn().forEach(this::spawn);
        Map<NexusId, NexusVisualRequest> requestsById = new LinkedHashMap<>();
        desiredVisuals.forEach(request -> requestsById.put(request.nexusId(), request));
        for (NexusVisualHandle handle : discoverVisualHandles()) {
            NexusVisualRequest request = requestsById.get(handle.nexusId());
            VisualState existing = visuals.get(handle.nexusId());
            if (request != null && (existing == null || !existing.handle.anchorEntityId().equals(handle.anchorEntityId()))) {
                attachRecoveredModel(request, handle).ifPresent(model ->
                        visuals.put(handle.nexusId(), new VisualState(request, handle, model)));
            }
        }
        return plan;
    }

    public synchronized void shutdown() {
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        List<NexusId> ids = List.copyOf(visuals.keySet());
        ids.forEach(this::remove);
    }

    private void spawnAuraParticles() {
        particleAngle += 0.2;
        if (particleAngle > Math.PI * 2) particleAngle -= Math.PI * 2;

        for (VisualState state : visuals.values()) {
            Entity entity = Bukkit.getEntity(state.handle.anchorEntityId());
            if (entity instanceof Ghast anchor && anchor.isValid()) {
                Location loc = anchor.getLocation().add(0, 1.5, 0);
                double r = 1.5;
                double x1 = r * Math.cos(particleAngle);
                double z1 = r * Math.sin(particleAngle);
                double x2 = r * Math.cos(particleAngle + Math.PI);
                double z2 = r * Math.sin(particleAngle + Math.PI);

                anchor.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(x1, 0, z1), 1, 0, 0.05, 0, 0);
                anchor.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(x2, 0, z2), 1, 0, 0.05, 0, 0);
            }
        }
    }

    private void configureAnchor(Ghast ghast, NexusId nexusId) {
        ClientEntityPresentationRegistry.register(ghast.getUniqueId(), "GHAST", "nexus_id");
        ghast.setAI(false);
        ghast.setAware(false);
        ghast.setGravity(false);
        ghast.setSilent(true);
        ghast.setPersistent(true);
        ghast.setRemoveWhenFarAway(false);
        ghast.setCollidable(false);
        ghast.setInvisible(true);
        ghast.setNoPhysics(true);
        ghast.setInvulnerable(false);
        ghast.setVelocity(new Vector());
        mark(ghast, nexusId, ROLE_ANCHOR);
    }

    private void configureChild(Entity entity, NexusId nexusId, String role) {
        entity.setGravity(false);
        entity.setPersistent(true);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setNoPhysics(true);
        mark(entity, nexusId, role);
    }

    private void mark(Entity entity, NexusId nexusId, String role) {
        entity.getPersistentDataContainer().set(nexusIdKey, PersistentDataType.STRING, nexusId.toString());
        entity.getPersistentDataContainer().set(visualRoleKey, PersistentDataType.STRING, role);
    }

    private Optional<JavaModelHandle> attachRecoveredModel(NexusVisualRequest request, NexusVisualHandle handle) {
        Entity entity = Bukkit.getEntity(handle.anchorEntityId());
        if (!(entity instanceof Ghast anchor) || !anchor.isValid()) {
            return Optional.empty();
        }
        JavaModelHandle model = models.attach(anchor, JavaModelKeys.NEXUS);
        models.animate(model, animationPolicy.steadyAnimation(healthFraction(request)));
        return Optional.of(model);
    }

    private static double healthFraction(NexusVisualRequest request) {
        return request.currentHealth() / request.maximumHealth();
    }

    private Optional<Ghast> anchor(NexusId nexusId) {
        VisualState state = visuals.get(nexusId);
        if (state != null) {
            Entity entity = Bukkit.getEntity(state.handle.anchorEntityId());
            if (entity instanceof Ghast ghast && entity.isValid()) {
                return Optional.of(ghast);
            }
        }
        return discoverVisualHandles().stream()
                .filter(handle -> handle.nexusId().equals(nexusId))
                .findFirst()
                .map(NexusVisualHandle::anchorEntityId)
                .map(Bukkit::getEntity)
                .filter(Ghast.class::isInstance)
                .map(Ghast.class::cast);
    }

    private <T extends Entity> Optional<T> entityWithRole(
            NexusVisualHandle handle,
            String wantedRole,
            Class<T> type
    ) {
        return handle.childEntityIds().stream()
                .map(Bukkit::getEntity)
                .filter(Objects::nonNull)
                .filter(entity -> wantedRole.equals(role(entity)))
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst();
    }

    private Optional<NexusId> readNexusId(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(nexusIdKey, PersistentDataType.STRING);
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(NexusId.parse(raw));
        } catch (IllegalArgumentException invalidId) {
            return Optional.empty();
        }
    }

    private String role(Entity entity) {
        return entity.getPersistentDataContainer().get(visualRoleKey, PersistentDataType.STRING);
    }

    private void removeEntity(UUID entityId) {
        ClientEntityPresentationRegistry.unregister(entityId);
        Entity entity = Bukkit.getEntity(entityId);
        if (entity != null) {
            entity.remove();
        }
    }

    private void removeDiscoveredEntities(NexusId nexusId) {
        for (World world : Bukkit.getWorlds()) {
            List<Entity> matching = world.getEntities().stream()
                    .filter(entity -> readNexusId(entity).filter(nexusId::equals).isPresent())
                    .toList();
            matching.forEach(entity -> {
                ClientEntityPresentationRegistry.unregister(entity.getUniqueId());
                entity.remove();
            });
        }
    }

    private static Location location(World world, NexusPosition position) {
        return new Location(world, position.x(), position.y(), position.z(), position.yaw(), 0.0f);
    }

    private static String renderHealth(String teamName, double health, double maximumHealth, boolean underAttack) {
        String state = underAttack ? "§cBAJO ATAQUE" : health / maximumHealth <= 0.25 ? "§cCRÍTICO" : "§aSEGURO";
        return teamName + "§l NEXUS\n"
                + "§f" + Math.round(health) + " §7/ §f" + Math.round(maximumHealth) + " HP\n"
                + state;
    }

    private static final class VisualState {
        private final NexusVisualRequest request;
        private final NexusVisualHandle handle;
        private final JavaModelHandle model;
        private double currentHealth;
        private String teamName;
        private boolean underAttack;

        private VisualState(NexusVisualRequest request, NexusVisualHandle handle, JavaModelHandle model) {
            this.request = request;
            this.handle = handle;
            this.model = model;
            this.currentHealth = request.currentHealth();
            this.teamName = request.teamName();
        }
    }
}
