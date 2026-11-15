package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.linqfy.bigCasares.dimension.NamespacedId;
import dev.linqfy.bigCasares.dimension.datapack.DatapackBuilder;
import dev.linqfy.bigCasares.dimension.datapack.GeneratedDatapack;
import dev.linqfy.bigCasares.dimension.json.Json;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.CompoundTag;
import dev.linqfy.bigCasares.dimension.worldgen.StructureDefinition;
import dev.linqfy.bigCasares.dimension.worldgen.StructureTemplate;
import dev.linqfy.bigCasares.dimension.worldgen.VerticalAnchor;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicExtraPiece.Role;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicPieces.RoomVariant;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicPieces.SideVariant;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicTemplateRewriter.Jigsaw;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds the {@code bigcasares-mythic-dungeons} datapack: one jigsaw structure per culture
 * ({@code bigc_myth:<culture>}), each in its own random-spread structure set, with biome tags
 * {@code bigc_myth:has_structure/<culture>}, template pools and code-built templates.
 *
 * <p>Main chain (every pool element of a stage leads to the next stage, so the arena is always
 * generated): {@code start} (surface entrance) -> {@code shaft} -> {@code hall_a} ->
 * {@code room_a} -> {@code stairs_a} -> {@code hall_b} -> {@code room_b} -> {@code stairs_b} ->
 * {@code arena}. Halls and rooms open onto {@code side_room}; the {@code seal} pool is the
 * fallback that plugs any doorway that could not receive a piece. A
 * {@link Role#FULL_DUNGEON} start uses {@code shaft_short} -> {@code stairs_b} -> {@code arena}.</p>
 *
 * <p>The build is pure and deterministic: the same sources give the same bytes and hash.</p>
 */
public final class MythicDungeonDatapack {

    public static final String FOLDER = "bigcasares-mythic-dungeons";
    public static final String DESCRIPTION = "BigCasares: mazmorras mitologicas";
    /** Weight of each code-built element in its pool; extra pieces choose their own. */
    public static final int BUILT_IN_WEIGHT = 4;
    public static final int SIDE_ROOM_HALF_WIDTH = 4;
    public static final int CORRIDOR_MAX_LENGTH = 11;
    public static final int CORRIDOR_MAX_LATERAL_OFFSET = 8;
    public static final int CORRIDOR_MAX_DROP = 8;
    public static final int SURFACE_ENTRANCE_MAX_CONNECTOR_OFFSET = 4;
    public static final int FULL_DUNGEON_MAX_CONNECTOR_OFFSET = 64;
    public static final int JIGSAW_SIZE = 12;
    /**
     * Vanilla rejects a jigsaw structure (and with it the whole datapack: the server does not start) when
     * {@code max_distance_from_center} plus the terrain-adaptation margin exceeds 128; every adaptation other than
     * {@code none} adds 12. The chain down to the arena needs the full 128, so the dungeons use no adaptation.
     */
    public static final int MAX_DISTANCE_FROM_CENTER = 128;
    public static final String TERRAIN_ADAPTATION = "none";
    static final int MAX_TEMPLATE_SIZE = 48;
    private static final String AIR = "minecraft:air";

    private MythicDungeonDatapack() {
    }

    /** The pack with only the code-built dungeons. */
    public static GeneratedDatapack build() {
        return build(List.of());
    }

    /**
     * The pack with extra pieces merged in. Throws {@link IllegalArgumentException} naming the
     * piece when a source supplies an invalid one.
     */
    public static GeneratedDatapack build(List<MythicTemplateSource> sources) {
        Objects.requireNonNull(sources, "sources");
        DatapackBuilder builder = new DatapackBuilder();
        for (MythicDungeonTheme theme : MythicDungeonTheme.all()) {
            List<MythicExtraPiece> extras = new ArrayList<>();
            for (MythicTemplateSource source : sources) {
                extras.addAll(Objects.requireNonNull(source.piecesFor(theme.culture()), "pieces"));
            }
            writeCulture(builder, theme, extras);
        }
        return builder.build(DESCRIPTION);
    }

    /** Pool ids of a culture; also used by tests. */
    static NamespacedId pool(MythicDungeonTheme theme, String name) {
        return theme.id(name);
    }

    private record Entry(StructureTemplate template, int weight) {
    }

    private static final class Pools {
        private final LinkedHashMap<NamespacedId, List<Entry>> entries = new LinkedHashMap<>();
        private final Map<NamespacedId, NamespacedId> fallbacks = new LinkedHashMap<>();

        void define(NamespacedId pool, NamespacedId fallback) {
            entries.put(pool, new ArrayList<>());
            fallbacks.put(pool, fallback);
        }

        void add(NamespacedId pool, StructureTemplate template, int weight) {
            entries.get(pool).add(new Entry(template, weight));
        }
    }

    private static void writeCulture(DatapackBuilder builder, MythicDungeonTheme theme, List<MythicExtraPiece> extras) {
        NamespacedId start = pool(theme, "start");
        NamespacedId shaft = pool(theme, "shaft");
        NamespacedId shaftShort = pool(theme, "shaft_short");
        NamespacedId hallA = pool(theme, "hall_a");
        NamespacedId roomA = pool(theme, "room_a");
        NamespacedId stairsA = pool(theme, "stairs_a");
        NamespacedId hallB = pool(theme, "hall_b");
        NamespacedId roomB = pool(theme, "room_b");
        NamespacedId stairsB = pool(theme, "stairs_b");
        NamespacedId arena = pool(theme, "arena");
        NamespacedId side = pool(theme, "side_room");
        NamespacedId seal = pool(theme, "seal");

        Pools pools = new Pools();
        for (NamespacedId id : List.of(start, shaft, shaftShort, arena, seal)) {
            pools.define(id, MythicPieces.EMPTY);
        }
        for (NamespacedId id : List.of(hallA, roomA, stairsA, hallB, roomB, stairsB, side)) {
            pools.define(id, seal);
        }

        StructureTemplate sealTemplate = MythicPieces.seal(theme);
        pools.add(start, MythicEntrances.entrance(theme, shaft), BUILT_IN_WEIGHT);
        pools.add(shaft, MythicPieces.shaft(theme, "shaft", hallA), 1);
        pools.add(shaftShort, MythicPieces.shaft(theme, "shaft_short", stairsB), 1);
        pools.add(hallA, MythicPieces.hall(theme, "hall_a", roomA, side), BUILT_IN_WEIGHT);
        for (RoomVariant variant : List.of(RoomVariant.CRYPT, RoomVariant.SHRINE, RoomVariant.PILLARED)) {
            pools.add(roomA, MythicPieces.room(theme, "room_a_" + lower(variant), variant, stairsA, side), BUILT_IN_WEIGHT);
        }
        pools.add(stairsA, MythicPieces.stairs(theme, "stairs_a", hallB), 1);
        pools.add(hallB, MythicPieces.hall(theme, "hall_b", roomB, side), BUILT_IN_WEIGHT);
        for (RoomVariant variant : RoomVariant.values()) {
            pools.add(roomB, MythicPieces.room(theme, "room_b_" + lower(variant), variant, stairsB, side), BUILT_IN_WEIGHT);
        }
        pools.add(stairsB, MythicPieces.stairs(theme, "stairs_b", arena), 1);
        pools.add(arena, MythicPieces.arena(theme), 1);
        for (SideVariant variant : SideVariant.values()) {
            pools.add(side, MythicPieces.sideRoom(theme, variant), BUILT_IN_WEIGHT);
        }
        pools.add(side, sealTemplate, BUILT_IN_WEIGHT - 1);
        pools.add(seal, sealTemplate, 1);

        Set<String> names = new HashSet<>();
        for (MythicExtraPiece extra : extras) {
            if (!names.add(extra.role() + "/" + extra.name())) {
                throw new IllegalArgumentException("Duplicate extra piece " + extra.name() + " for " + theme.culture());
            }
            ExtraContext context = new ExtraContext(theme, shaft, shaftShort, side, seal);
            switch (extra.role()) {
                case SIDE_ROOM -> pools.add(side, sideRoom(context, extra), extra.weight());
                case CORRIDOR -> {
                    pools.add(hallA, corridor(context, extra, "corridor_a", roomA), extra.weight());
                    pools.add(hallB, corridor(context, extra, "corridor_b", roomB), extra.weight());
                }
                case SURFACE_ENTRANCE -> pools.add(start, surface(context, extra, false), extra.weight());
                case FULL_DUNGEON -> pools.add(start, surface(context, extra, true), extra.weight());
            }
        }

        for (Map.Entry<NamespacedId, List<Entry>> pool : pools.entries.entrySet()) {
            JsonArray elements = new JsonArray();
            for (Entry entry : pool.getValue()) {
                builder.structureTemplate(entry.template().id(), entry.template().toBytes());
                elements.add(Json.object()
                    .put("element", Json.object()
                        .put("element_type", "minecraft:single_pool_element")
                        .put("location", entry.template().id())
                        .put("processors", "minecraft:empty")
                        .put("projection", "rigid")
                        .build())
                    .put("weight", entry.weight())
                    .build());
            }
            JsonObject json = Json.object()
                .put("elements", elements)
                .put("fallback", pools.fallbacks.get(pool.getKey()))
                .build();
            builder.json(DatapackBuilder.Registry.TEMPLATE_POOL, pool.getKey(), json);
        }

        StructureDefinition definition = definition(theme, pools.entries.get(start).getFirst().template(), start);
        builder.json(DatapackBuilder.Registry.STRUCTURE, definition.id(), definition.toJson());
        builder.json(DatapackBuilder.Registry.STRUCTURE_SET, definition.id(), definition.structureSetJson());
        builder.tag("worldgen/biome", definition.biomeTag(), theme.biomes());
    }

    static StructureDefinition definition(MythicDungeonTheme theme, StructureTemplate startTemplate, NamespacedId startPool) {
        StructureDefinition.TemplatePool pool = new StructureDefinition.TemplatePool(startPool,
            List.of(new StructureDefinition.PoolElement(startTemplate, null, "rigid", BUILT_IN_WEIGHT)));
        MythicDungeonTheme.Spread spread = theme.spread();
        // Start at the surface (WORLD_SURFACE_WG); start_height 0 puts template layer y = 0 on the
        // top terrain block. No terrain adaptation (see TERRAIN_ADAPTATION).
        return new StructureDefinition(theme.structureId(),
            theme.biomes().stream().map(NamespacedId::parse).toList(),
            pool, List.of(), "surface_structures", JIGSAW_SIZE, VerticalAnchor.absolute(0), "WORLD_SURFACE_WG",
            MAX_DISTANCE_FROM_CENTER, TERRAIN_ADAPTATION,
            new StructureDefinition.Placement(spread.spacing(), spread.separation(), spread.salt()));
    }

    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(java.util.Locale.ROOT);
    }

    private record ExtraContext(MythicDungeonTheme theme, NamespacedId shaft, NamespacedId shaftShort,
                                NamespacedId side, NamespacedId seal) {
        NamespacedId door() {
            return MythicPieces.door(theme);
        }
    }

    private static IllegalArgumentException invalid(MythicDungeonTheme theme, MythicExtraPiece extra, String reason) {
        return new IllegalArgumentException("Extra piece " + extra.name() + " (" + extra.role() + ") for "
            + theme.culture() + ": " + reason);
    }

    private static Jigsaw connector(MythicDungeonTheme theme, MythicExtraPiece extra, List<Jigsaw> jigsaws, String name) {
        List<Jigsaw> matches = jigsaws.stream().filter(jigsaw -> jigsaw.name().equals(name)).toList();
        if (matches.size() != 1) {
            throw invalid(theme, extra, "expected exactly one jigsaw named " + name + ", found " + matches.size());
        }
        return matches.getFirst();
    }

    private static int[] validated(MythicDungeonTheme theme, MythicExtraPiece extra) {
        List<String> foreign = MythicTemplateRewriter.nonVanillaIds(extra.template());
        if (!foreign.isEmpty()) {
            throw invalid(theme, extra, "non-vanilla ids " + foreign);
        }
        int[] size = MythicTemplateRewriter.size(extra.template());
        for (int axis : size) {
            if (axis < 1 || axis > MAX_TEMPLATE_SIZE) {
                throw invalid(theme, extra, "template size must be within 1.." + MAX_TEMPLATE_SIZE + " per axis");
            }
        }
        return size;
    }

    private static String finalState(Jigsaw jigsaw) {
        String state = jigsaw.nbt() == null ? null : jigsaw.nbt().getString("final_state");
        return state == null || state.isBlank() ? AIR : state;
    }

    private static CompoundTag neutral(Jigsaw jigsaw) {
        return MythicTemplateRewriter.jigsawNbt(MythicPieces.EMPTY, MythicPieces.EMPTY, MythicPieces.EMPTY, finalState(jigsaw), 0);
    }

    private static CompoundTag sealed(ExtraContext context, Jigsaw jigsaw) {
        return MythicTemplateRewriter.jigsawNbt(MythicPieces.EMPTY, context.door(), context.seal(), finalState(jigsaw), 0);
    }

    private static CompoundTag entryNbt(ExtraContext context) {
        return MythicTemplateRewriter.jigsawNbt(context.door(), MythicPieces.EMPTY, MythicPieces.EMPTY, AIR, 0);
    }

    /** Position along the face plane (x for north/south faces, z for east/west faces). */
    private static int alongFace(Jigsaw jigsaw) {
        return jigsaw.front().equals("north") || jigsaw.front().equals("south") ? jigsaw.x() : jigsaw.z();
    }

    private static int faceLength(Jigsaw jigsaw, int[] size) {
        return jigsaw.front().equals("north") || jigsaw.front().equals("south") ? size[0] : size[2];
    }

    private static StructureTemplate sideRoom(ExtraContext context, MythicExtraPiece extra) {
        MythicDungeonTheme theme = context.theme();
        int[] size = validated(theme, extra);
        List<Jigsaw> jigsaws = MythicTemplateRewriter.jigsaws(extra.template());
        Jigsaw entry = connector(theme, extra, jigsaws, extra.entryConnector());
        if (!entry.horizontal()) {
            throw invalid(theme, extra, "entry connector must face horizontally");
        }
        int position = alongFace(entry);
        if (position > SIDE_ROOM_HALF_WIDTH || faceLength(entry, size) - 1 - position > SIDE_ROOM_HALF_WIDTH) {
            throw invalid(theme, extra, "extends more than " + SIDE_ROOM_HALF_WIDTH + " blocks beside its entry");
        }
        return MythicTemplateRewriter.rewrite(extra.template(), theme.id("extra/side_room/" + extra.name()),
            jigsaw -> jigsaw.index() == entry.index() ? entryNbt(context) : sealed(context, jigsaw), Set.of());
    }

    private static StructureTemplate corridor(ExtraContext context, MythicExtraPiece extra, String stage, NamespacedId next) {
        MythicDungeonTheme theme = context.theme();
        int[] size = validated(theme, extra);
        List<Jigsaw> jigsaws = MythicTemplateRewriter.jigsaws(extra.template());
        Jigsaw entry = connector(theme, extra, jigsaws, extra.entryConnector());
        Jigsaw exit = connector(theme, extra, jigsaws, extra.exitConnector());
        if (!entry.horizontal() || !exit.horizontal() || !opposite(entry.front()).equals(exit.front())) {
            throw invalid(theme, extra, "entry and exit must face horizontally in opposite directions");
        }
        boolean alongZ = entry.front().equals("north") || entry.front().equals("south");
        int length = alongZ ? size[2] : size[0];
        int lateral = Math.abs(alongZ ? entry.x() - exit.x() : entry.z() - exit.z());
        int drop = entry.y() - exit.y();
        if (length > CORRIDOR_MAX_LENGTH || lateral > CORRIDOR_MAX_LATERAL_OFFSET || drop < 0 || drop > CORRIDOR_MAX_DROP) {
            throw invalid(theme, extra, "length " + length + ", lateral offset " + lateral + " or drop " + drop
                + " outside the limits that keep the arena in range");
        }
        NamespacedId door = context.door();
        return MythicTemplateRewriter.rewrite(extra.template(), theme.id("extra/" + stage + "/" + extra.name()), jigsaw -> {
            if (jigsaw.index() == entry.index()) {
                return entryNbt(context);
            }
            if (jigsaw.index() == exit.index()) {
                return MythicTemplateRewriter.jigsawNbt(MythicPieces.EMPTY, door, next, AIR, 1);
            }
            if (!jigsaw.horizontal()) {
                return neutral(jigsaw);
            }
            boolean sideways = alongZ ? jigsaw.front().equals("east") || jigsaw.front().equals("west")
                : jigsaw.front().equals("north") || jigsaw.front().equals("south");
            int axial = alongZ ? jigsaw.z() : jigsaw.x();
            boolean fits = axial - SIDE_ROOM_HALF_WIDTH >= 0 && axial + SIDE_ROOM_HALF_WIDTH <= length - 1;
            return sideways && fits
                ? MythicTemplateRewriter.jigsawNbt(MythicPieces.EMPTY, door, context.side(), finalState(jigsaw), 0)
                : sealed(context, jigsaw);
        }, Set.of());
    }

    private static StructureTemplate surface(ExtraContext context, MythicExtraPiece extra, boolean fullDungeon) {
        MythicDungeonTheme theme = context.theme();
        int[] size = validated(theme, extra);
        List<Jigsaw> jigsaws = MythicTemplateRewriter.jigsaws(extra.template());
        Jigsaw descent = connector(theme, extra, jigsaws, extra.entryConnector());
        int limit = fullDungeon ? FULL_DUNGEON_MAX_CONNECTOR_OFFSET : SURFACE_ENTRANCE_MAX_CONNECTOR_OFFSET;
        if (!descent.front().equals("down") || descent.y() != 0) {
            throw invalid(theme, extra, "entry connector must face down in the bottom layer");
        }
        double offsetX = Math.abs(descent.x() - (size[0] - 1) / 2.0);
        double offsetZ = Math.abs(descent.z() - (size[2] - 1) / 2.0);
        if (offsetX > limit || offsetZ > limit) {
            throw invalid(theme, extra, "entry connector is more than " + limit + " blocks from the centre");
        }
        Set<List<Integer>> opening = new HashSet<>();
        for (int x = descent.x() - 1; x <= descent.x() + 1; x++) {
            for (int z = descent.z() - 1; z <= descent.z() + 1; z++) {
                for (int y = 0; y <= Math.min(3, size[1] - 1); y++) {
                    if (x >= 0 && z >= 0 && x < size[0] && z < size[2] && !(x == descent.x() && y == 0 && z == descent.z())) {
                        opening.add(List.of(x, y, z));
                    }
                }
            }
        }
        NamespacedId target = fullDungeon ? context.shaftShort() : context.shaft();
        String role = fullDungeon ? "full_dungeon" : "entrance";
        return MythicTemplateRewriter.rewrite(extra.template(), theme.id("extra/" + role + "/" + extra.name()), jigsaw -> {
            if (jigsaw.index() == descent.index()) {
                return MythicTemplateRewriter.jigsawNbt(MythicPieces.EMPTY, MythicPieces.shaftTop(theme), target, AIR, 1);
            }
            if (fullDungeon && jigsaw.horizontal()) {
                return MythicTemplateRewriter.jigsawNbt(MythicPieces.EMPTY, context.door(), context.side(), finalState(jigsaw), 0);
            }
            return neutral(jigsaw);
        }, opening);
    }

    private static String opposite(String direction) {
        return switch (direction) {
            case "north" -> "south";
            case "south" -> "north";
            case "east" -> "west";
            case "west" -> "east";
            case "up" -> "down";
            default -> "up";
        };
    }
}
