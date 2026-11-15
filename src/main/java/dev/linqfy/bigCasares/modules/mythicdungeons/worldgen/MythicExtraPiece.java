package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import dev.linqfy.bigCasares.dimension.worldgen.StructureTemplate;

import java.util.Objects;

/**
 * One extra template for a culture's dungeon, see {@link MythicTemplateSource}.
 *
 * <p>{@code name} becomes part of the template id ({@code bigc_myth:<culture>/extra/<role>/<name>})
 * and must be a lowercase resource path, unique per culture and role. {@code weight} (1..150) is the
 * pool weight next to the built-in pieces ({@link MythicDungeonDatapack#BUILT_IN_WEIGHT} each).
 * Connector names are the {@code name} field of jigsaw blocks inside the template.</p>
 *
 * <p>Roles and how jigsaws are rewritten (horizontal = facing north/south/east/west):</p>
 * <ul>
 *     <li>{@link Role#SIDE_ROOM}: dead-end room off a hall or room doorway. {@code entryConnector}
 *     must be a horizontal jigsaw; it becomes the entry. Along the face that holds the entry,
 *     the template may extend at most {@link MythicDungeonDatapack#SIDE_ROOM_HALF_WIDTH} blocks to
 *     either side of the connector (so at most 9 wide), which keeps it within the parent's span and
 *     away from the next piece of the main chain. Every other jigsaw is sealed
 *     (its pool becomes the culture's seal pool).</li>
 *     <li>{@link Role#CORRIDOR}: alternative to the code-built halls. {@code entryConnector} and
 *     {@code exitConnector} must be horizontal jigsaws facing opposite ways; the exit continues the
 *     main chain (it is used for both hall stages). Length along the chain at most {@link MythicDungeonDatapack#CORRIDOR_MAX_LENGTH},
 *     exit at most 8 blocks lower and never higher than the entry, lateral offset at most 8 — this
 *     keeps the arena inside the jigsaw distance limit. Other horizontal jigsaws whose position lets a
 *     9-wide side room stay within the corridor's length become side doorways; the rest are sealed.</li>
 *     <li>{@link Role#SURFACE_ENTRANCE}: alternative start piece. {@code entryConnector} must face down
 *     ({@code down_*}) in the bottom layer within
 *     {@link MythicDungeonDatapack#SURFACE_ENTRANCE_MAX_CONNECTOR_OFFSET} blocks of the template centre
 *     on each horizontal axis; the full code-built
 *     dungeon hangs below it. The builder clears a 3x3, 4-tall opening above the connector. Other
 *     jigsaws are neutralised (they just become their final state).</li>
 *     <li>{@link Role#FULL_DUNGEON}: a complete surface building used as alternative start. Its
 *     {@code entryConnector} (facing down, bottom layer, within
 *     {@link MythicDungeonDatapack#FULL_DUNGEON_MAX_CONNECTOR_OFFSET} of the centre) leads to a short
 *     descent straight to the boss arena; its other horizontal jigsaws open onto the culture's side
 *     rooms (sealed when nothing fits), vertical ones are neutralised.</li>
 * </ul>
 * {@code exitConnector} is only used (and required) by {@link Role#CORRIDOR}. Connectors of the
 * surface roles get selection priority 1, so the descent is always placed before anything else.
 * Every template must stay within 48 blocks per axis and use only {@code minecraft:} ids.
 */
public record MythicExtraPiece(
    String name,
    StructureTemplate template,
    Role role,
    String entryConnector,
    String exitConnector,
    int weight
) {

    public enum Role {
        SIDE_ROOM, CORRIDOR, SURFACE_ENTRANCE, FULL_DUNGEON
    }

    public MythicExtraPiece {
        Objects.requireNonNull(name, "name");
        if (!name.matches("[a-z0-9_]+(/[a-z0-9_]+)*")) {
            throw new IllegalArgumentException("Invalid extra piece name: " + name);
        }
        Objects.requireNonNull(template, "template");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(entryConnector, "entryConnector");
        if (role == Role.CORRIDOR && exitConnector == null) {
            throw new IllegalArgumentException("Corridor " + name + " needs an exit connector");
        }
        if (weight < 1 || weight > 150) {
            throw new IllegalArgumentException("Extra piece weight must be within 1..150");
        }
    }
}
