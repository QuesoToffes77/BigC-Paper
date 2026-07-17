package dev.linqfy.bigCasares.modules.nexus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class NexusPlacementPolicy {

    private static final int HITBOX_HORIZONTAL_RADIUS = 1;
    private static final int HITBOX_HEIGHT = 3;
    private static final int MINIMUM_PLACEMENT_Y = -10;
    private static final int MAXIMUM_PLACEMENT_Y = 150;

    private static final List<Face> CARDINAL_FACES = List.of(
            new Face(0, -1),
            new Face(1, 0),
            new Face(0, 1),
            new Face(-1, 0)
    );

    private final NexusPlacementSettings settings;

    public NexusPlacementPolicy(NexusPlacementSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    public NexusPlacementSettings settings() {
        return settings;
    }

    public NexusPlacementResult validateNexusPlacement(
            NexusBlockPosition origin,
            NexusPlacementProbe probe
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(probe, "probe");

        if (origin.y() < MINIMUM_PLACEMENT_Y || origin.y() > MAXIMUM_PLACEMENT_Y) {
            return NexusPlacementResult.rejected(
                    NexusPlacementRejection.Y_OUT_OF_RANGE,
                    origin,
                    null
            );
        }

        NexusPlacementResult containerResult = findBlockingContainer(origin, probe);
        if (!containerResult.allowed()) {
            return containerResult;
        }

        for (int y = 0; y < HITBOX_HEIGHT; y++) {
            for (int x = -HITBOX_HORIZONTAL_RADIUS; x <= HITBOX_HORIZONTAL_RADIUS; x++) {
                for (int z = -HITBOX_HORIZONTAL_RADIUS; z <= HITBOX_HORIZONTAL_RADIUS; z++) {
                    NexusBlockPosition hitboxPosition = origin.offset(x, y, z);
                    if (!probe.isPassable(hitboxPosition)) {
                        return NexusPlacementResult.rejected(
                                NexusPlacementRejection.HITBOX_OBSTRUCTED,
                                hitboxPosition,
                                probe.materialAt(hitboxPosition)
                        );
                    }
                }
            }
        }

        List<Face> openFaces = openFaces(origin, probe);
        if (openFaces.size() < settings.minimumOpenFaces()) {
            return NexusPlacementResult.rejected(
                    NexusPlacementRejection.INSUFFICIENT_OPEN_FACES,
                    origin,
                    null
            );
        }

        if (settings.preventDoorwayPlacement()
                && settings.minimumWalkableWidth() > 1
                && openFaces.size() == 2
                && openFaces.get(0).isOpposite(openFaces.get(1))) {
            return NexusPlacementResult.rejected(NexusPlacementRejection.DOORWAY, origin, null);
        }

        return NexusPlacementResult.allowedResult();
    }

    public NexusPlacementResult findBlockingContainer(
            NexusBlockPosition origin,
            NexusPlacementProbe probe
    ) {
        int horizontal = settings.containerClearanceHorizontal();
        int vertical = settings.containerClearanceVertical();
        for (int y = -vertical; y <= vertical; y++) {
            for (int x = -horizontal; x <= horizontal; x++) {
                for (int z = -horizontal; z <= horizontal; z++) {
                    NexusBlockPosition position = origin.offset(x, y, z);
                    String material = probe.materialAt(position);
                    if (NexusContainerMaterials.isContainer(material)) {
                        return NexusPlacementResult.rejected(
                                NexusPlacementRejection.CONTAINER_TOO_CLOSE,
                                position,
                                material
                        );
                    }
                }
            }
        }
        return NexusPlacementResult.allowedResult();
    }

    public NexusPlacementResult validateContainerPlacement(
            NexusBlockPosition placedContainer,
            Collection<NexusBlockPosition> activeNexuses,
            Optional<NexusBlockPosition> connectedContainer
    ) {
        Objects.requireNonNull(placedContainer, "placedContainer");
        Objects.requireNonNull(activeNexuses, "activeNexuses");
        Objects.requireNonNull(connectedContainer, "connectedContainer");

        Optional<NexusBlockPosition> blocking = protectedPosition(placedContainer, activeNexuses);
        if (blocking.isEmpty() && connectedContainer.isPresent()) {
            NexusBlockPosition connected = connectedContainer.orElseThrow();
            if (protectedPosition(connected, activeNexuses).isPresent()) {
                blocking = Optional.of(connected);
            }
        }
        return blocking
                .map(position -> NexusPlacementResult.rejected(
                        NexusPlacementRejection.PROTECTED_NEXUS_VOLUME,
                        position,
                        null
                ))
                .orElseGet(NexusPlacementResult::allowedResult);
    }

    public NexusPlacementResult validatePistonMove(
            NexusBlockPosition source,
            NexusBlockPosition destination,
            Collection<NexusBlockPosition> activeNexuses
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(activeNexuses, "activeNexuses");
        return protectedPosition(destination, activeNexuses)
                .map(position -> NexusPlacementResult.rejected(
                        NexusPlacementRejection.PROTECTED_NEXUS_VOLUME,
                        destination,
                        null
                ))
                .orElseGet(NexusPlacementResult::allowedResult);
    }

    public boolean isInsideProtectedVolume(
            NexusBlockPosition nexus,
            NexusBlockPosition candidate
    ) {
        int horizontal = settings.containerClearanceHorizontal();
        int vertical = settings.containerClearanceVertical();
        return Math.abs(candidate.x() - nexus.x()) <= horizontal
                && Math.abs(candidate.z() - nexus.z()) <= horizontal
                && Math.abs(candidate.y() - nexus.y()) <= vertical;
    }

    public int countOpenFaces(NexusBlockPosition origin, NexusPlacementProbe probe) {
        return openFaces(origin, probe).size();
    }

    private Optional<NexusBlockPosition> protectedPosition(
            NexusBlockPosition candidate,
            Collection<NexusBlockPosition> activeNexuses
    ) {
        return activeNexuses.stream()
                .filter(nexus -> isInsideProtectedVolume(nexus, candidate))
                .findFirst();
    }

    public Optional<NexusBlockPosition> protectedNexus(NexusBlockPosition candidate, Collection<NexusBlockPosition> activeNexuses) {
        return protectedPosition(candidate, activeNexuses);
    }

    private List<Face> openFaces(NexusBlockPosition origin, NexusPlacementProbe probe) {
        List<Face> open = new ArrayList<>();
        for (Face face : CARDINAL_FACES) {
            if (isWalkableFace(origin, probe, face)) {
                open.add(face);
            }
        }
        return open;
    }

    private boolean isWalkableFace(
            NexusBlockPosition origin,
            NexusPlacementProbe probe,
            Face face
    ) {
        int distance = HITBOX_HORIZONTAL_RADIUS + 1;
        int left = (settings.minimumWalkableWidth() - 1) / 2;
        int right = settings.minimumWalkableWidth() / 2;
        int lateralX = -face.z();
        int lateralZ = face.x();
        for (int lateral = -left; lateral <= right; lateral++) {
            int x = face.x() * distance + lateralX * lateral;
            int z = face.z() * distance + lateralZ * lateral;
            for (int y = 0; y <= 1; y++) {
                if (!probe.isPassable(origin.offset(x, y, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    private record Face(int x, int z) {
        private boolean isOpposite(Face other) {
            return x == -other.x && z == -other.z;
        }
    }
}
