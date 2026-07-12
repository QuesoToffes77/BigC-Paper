package dev.linqfy.bigCasares.modules.nexus;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class NexusListener implements Listener {

    private static final List<BlockFace> HORIZONTAL_FACES = List.of(
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST
    );

    private final NexusService service;
    private final NexusDamageResolver damageResolver;
    private final NexusPlacementPolicy placementPolicy;
    private final JavaNexusVisualGateway visualGateway;
    private final Consumer<NexusId> destroyedCallback;
    private final java.util.function.BiConsumer<NexusId, java.util.UUID> attackedCallback;
    private final java.util.function.BiPredicate<Block, java.util.UUID> containerPlacement;

    public NexusListener(
            NexusService service,
            NexusDamageResolver damageResolver,
            NexusPlacementPolicy placementPolicy,
            JavaNexusVisualGateway visualGateway
    ) {
        this(service, damageResolver, placementPolicy, visualGateway, ignored -> { }, (id, player) -> { }, (block, player) -> false);
    }

    public NexusListener(
            NexusService service,
            NexusDamageResolver damageResolver,
            NexusPlacementPolicy placementPolicy,
            JavaNexusVisualGateway visualGateway,
        Consumer<NexusId> destroyedCallback
    ) {
        this(service, damageResolver, placementPolicy, visualGateway, destroyedCallback, (id, player) -> { }, (block, player) -> false);
    }

    public NexusListener(NexusService service, NexusDamageResolver damageResolver, NexusPlacementPolicy placementPolicy,
                         JavaNexusVisualGateway visualGateway, Consumer<NexusId> destroyedCallback,
                         java.util.function.BiConsumer<NexusId, java.util.UUID> attackedCallback,
                         java.util.function.BiPredicate<Block, java.util.UUID> containerPlacement) {
        this.service = Objects.requireNonNull(service, "service");
        this.damageResolver = Objects.requireNonNull(damageResolver, "damageResolver");
        this.placementPolicy = Objects.requireNonNull(placementPolicy, "placementPolicy");
        this.visualGateway = Objects.requireNonNull(visualGateway, "visualGateway");
        this.destroyedCallback = Objects.requireNonNull(destroyedCallback, "destroyedCallback");
        this.attackedCallback = Objects.requireNonNull(attackedCallback, "attackedCallback");
        this.containerPlacement = Objects.requireNonNull(containerPlacement, "containerPlacement");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNexusDamage(EntityDamageEvent event) {
        Optional<NexusId> nexusId = visualGateway.findNexusId(event.getEntity());
        if (nexusId.isEmpty()) {
            return;
        }

        NexusDamageRequest request = damageResolver.resolve(nexusId.orElseThrow(), event);
        event.setCancelled(true);
        NexusDamageResult result = service.applyDamage(request);
        if (result.outcome() == NexusDamageOutcome.APPLIED && request.attackerId().isPresent()) {
            attackedCallback.accept(request.nexusId(), request.attackerId().orElseThrow());
        }
        if (result.outcome() == NexusDamageOutcome.DESTROYED) {
            destroyedCallback.accept(request.nexusId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onContainerPlace(BlockPlaceEvent event) {
        Material placedMaterial = event.getBlockPlaced().getType();
        if (!NexusContainerMaterials.isContainer(placedMaterial)) {
            return;
        }

        Block placed = event.getBlockPlaced();
        if (placementPolicy.protectedNexus(position(placed), activeNexusPositions(placed.getWorld())).isPresent()
                && !containerPlacement.test(placed, event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cSolo el equipo dueño puede colocar contenedores en este Nexus.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (movesContainerIntoProtectedArea(event.getBlocks(), event.getDirection(), event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        BlockFace movement = event.getDirection().getOppositeFace();
        if (movesContainerIntoProtectedArea(event.getBlocks(), movement, event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    public NexusPlacementResult validateNexusPlacement(Location location) {
        Objects.requireNonNull(location, "location");
        World world = Objects.requireNonNull(location.getWorld(), "location world");
        return placementPolicy.validateNexusPlacement(
                new NexusBlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()),
                new BukkitNexusPlacementProbe(world)
        );
    }

    public NexusPlacementResult placeNexus(NexusVisualRequest request) {
        World world = org.bukkit.Bukkit.getWorld(request.position().worldId());
        if (world == null) {
            return NexusPlacementResult.rejected(
                    NexusPlacementRejection.HITBOX_OBSTRUCTED,
                    request.position().blockPosition(),
                    "WORLD_NOT_LOADED"
            );
        }
        // Ensure no overlap with any other Nexus's protected volume
        Collection<NexusBlockPosition> activeNexuses = activeNexusPositions(world);
        for (NexusBlockPosition active : activeNexuses) {
            if (placementPolicy.isInsideProtectedVolume(active, request.position().blockPosition())) {
                return NexusPlacementResult.rejected(
                        NexusPlacementRejection.PROTECTED_NEXUS_VOLUME,
                        active,
                        null
                );
            }
        }
        NexusPlacementResult placement = placementPolicy.validateNexusPlacement(
                request.position().blockPosition(),
                new BukkitNexusPlacementProbe(world)
        );
        if (placement.allowed()) {
            service.register(request);
        }
        return placement;
    }

    private boolean movesContainerIntoProtectedArea(
            List<Block> movedBlocks,
            BlockFace movement,
            World world
    ) {
        Collection<NexusBlockPosition> activeNexuses = activeNexusPositions(world);
        for (Block block : movedBlocks) {
            if (!NexusContainerMaterials.isContainer(block.getType())) {
                continue;
            }
            NexusBlockPosition source = position(block);
            NexusBlockPosition destination = position(block.getRelative(movement));
            if (!placementPolicy.validatePistonMove(source, destination, activeNexuses).allowed()) {
                return true;
            }
        }
        return false;
    }

    private Collection<NexusBlockPosition> activeNexusPositions(World world) {
        return service.activePositions().stream()
                .filter(position -> position.worldId().equals(world.getUID()))
                .map(NexusPosition::blockPosition)
                .toList();
    }

    private Optional<NexusBlockPosition> connectedChestPosition(Block placed) {
        Material material = placed.getType();
        if (material != Material.CHEST && material != Material.TRAPPED_CHEST) {
            return Optional.empty();
        }
        return HORIZONTAL_FACES.stream()
                .map(placed::getRelative)
                .filter(block -> block.getType() == material)
                .findFirst()
                .map(NexusListener::position);
    }

    private static NexusBlockPosition position(Block block) {
        return new NexusBlockPosition(block.getX(), block.getY(), block.getZ());
    }
}
