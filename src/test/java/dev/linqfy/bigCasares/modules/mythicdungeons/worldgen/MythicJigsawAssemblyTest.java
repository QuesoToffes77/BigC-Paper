package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import dev.linqfy.bigCasares.dimension.NamespacedId;
import dev.linqfy.bigCasares.dimension.datapack.GeneratedDatapack;
import dev.linqfy.bigCasares.dimension.worldgen.BlockStateSpec;
import dev.linqfy.bigCasares.dimension.worldgen.StructureTemplate;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicExtraPiece.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MythicJigsawAssemblyTest {

    private static final int SURFACE_Y = 62;
    private static final int WORLD_MIN_Y = -64;
    private static final List<String> MAIN_CHAIN = List.of("entrance", "shaft", "hall_a", "room_a_", "stairs_a",
        "hall_b", "room_b_", "stairs_b", "arena");

    @Test
    void arenaIsAlwaysReachedAndPiecesNeverOverlap() {
        MythicPackIndex index = new MythicPackIndex(MythicDungeonDatapack.build());
        MythicJigsawSimulator simulator = new MythicJigsawSimulator(index, MythicDungeonDatapack.JIGSAW_SIZE,
            MythicDungeonDatapack.MAX_DISTANCE_FROM_CENTER);
        for (MythicDungeonTheme theme : MythicDungeonTheme.all()) {
            for (long seed = 0; seed < 40; seed++) {
                List<MythicJigsawSimulator.Placed> pieces = simulator.assemble(theme.id("start").asString(), seed, SURFACE_Y, WORLD_MIN_Y);
                List<String> paths = pieces.stream().map(piece -> piece.template().id().path()).toList();
                for (String stage : MAIN_CHAIN) {
                    assertEquals(1, paths.stream().filter(path -> path.contains("/pieces/" + stage)).count(),
                        theme.culture() + " seed " + seed + " stage " + stage + " in " + paths);
                }
                for (int a = 0; a < pieces.size(); a++) {
                    for (int b = a + 1; b < pieces.size(); b++) {
                        assertTrue(!pieces.get(a).box().intersects(pieces.get(b).box()), "overlap in " + theme.culture());
                    }
                }
                MythicJigsawSimulator.Placed arena = pieces.stream()
                    .filter(piece -> piece.template().id().path().endsWith("/pieces/arena")).findFirst().orElseThrow();
                assertTrue(arena.box().maxY() < SURFACE_Y - 10, "arena stays underground");
                long rooms = paths.stream().filter(path -> path.contains("/pieces/room_") || path.contains("/pieces/hall_")
                    || path.contains("/pieces/side_")).count();
                assertTrue(rooms >= 4 && rooms <= 12, theme.culture() + " rooms " + rooms);
            }
        }
    }

    @Test
    void extraPiecesMergeAndKeepTheArenaReachable() {
        MythicTemplateSource source = culture -> culture.equals("greek") ? fakePieces() : List.of();
        GeneratedDatapack pack = MythicDungeonDatapack.build(List.of(source));
        assertNotEquals(MythicDungeonDatapack.build().contentHash(), pack.contentHash());
        assertEquals(pack.contentHash(), MythicDungeonDatapack.build(List.of(source)).contentHash());
        MythicPackIndex index = new MythicPackIndex(pack);
        Set<String> extras = index.templates.keySet().stream().filter(id -> id.contains("/extra/")).collect(Collectors.toSet());
        assertEquals(Set.of("bigc_myth:greek/extra/side_room/fake_room", "bigc_myth:greek/extra/corridor_a/fake_corridor",
            "bigc_myth:greek/extra/corridor_b/fake_corridor", "bigc_myth:greek/extra/entrance/fake_entrance",
            "bigc_myth:greek/extra/full_dungeon/fake_castle"), extras);
        assertTrue(index.templates.keySet().stream().noneMatch(id -> id.startsWith("bigc_myth:norse/extra")));

        MythicJigsawSimulator simulator = new MythicJigsawSimulator(index, MythicDungeonDatapack.JIGSAW_SIZE,
            MythicDungeonDatapack.MAX_DISTANCE_FROM_CENTER);
        int fullDungeons = 0;
        int extraCorridors = 0;
        for (long seed = 0; seed < 120; seed++) {
            List<MythicJigsawSimulator.Placed> pieces = simulator.assemble("bigc_myth:greek/start", seed, SURFACE_Y, WORLD_MIN_Y);
            List<String> paths = pieces.stream().map(piece -> piece.template().id().path()).toList();
            assertEquals(1, paths.stream().filter(path -> path.endsWith("/pieces/arena")).count(), "seed " + seed + " " + paths);
            if (paths.contains("greek/extra/full_dungeon/fake_castle")) {
                fullDungeons++;
                assertTrue(paths.contains("greek/pieces/shaft_short"));
            }
            extraCorridors += (int) paths.stream().filter(path -> path.contains("/extra/corridor_")).count();
        }
        assertTrue(fullDungeons > 0 && extraCorridors > 0, "extras were exercised");
    }

    @Test
    void invalidExtraPiecesAreRejectedAndSkippedByTheInstaller() {
        MythicCanvas canvas = new MythicCanvas(NamespacedId.of("fake", "modded"), MythicDungeonTheme.GREEK, 5, 5, 5);
        canvas.set(2, 2, 2, BlockStateSpec.of("othermod:magic_block"));
        canvas.jigsaw(2, 1, 0, "north_up", NamespacedId.of("fake", "entry"), MythicPieces.EMPTY, MythicPieces.EMPTY, MythicCanvas.AIR, 0);
        MythicTemplateSource modded = culture -> List.of(new MythicExtraPiece("modded", canvas.build(), Role.SIDE_ROOM,
            "fake:entry", null, 5));
        assertThrows(IllegalArgumentException.class, () -> MythicDungeonDatapack.build(List.of(modded)));

        MythicTemplateSource tooWide = culture -> List.of(new MythicExtraPiece("wide", box("wide", 13, 5, 6,
            "fake:entry", 1, 1, 0, "north_up"), Role.SIDE_ROOM, "fake:entry", null, 5));
        assertThrows(IllegalArgumentException.class, () -> MythicDungeonDatapack.build(List.of(tooWide)));

        MythicTemplateSource broken = culture -> {
            throw new IllegalStateException("cannot read templates");
        };
        MythicTemplateSource good = culture -> culture.equals("greek") ? fakePieces() : List.of();
        List<MythicTemplateSource> usable = MythicDatapackInstaller.usableSources(List.of(broken, modded, good),
            Logger.getAnonymousLogger());
        assertEquals(List.of(good), usable);
    }

    private static List<MythicExtraPiece> fakePieces() {
        StructureTemplate room = box("fake_room", 9, 6, 7, "fake:entry", 4, 1, 0, "north_up");
        StructureTemplate corridor = withJigsaws("fake_corridor", 7, 6, 9,
            new String[][]{{"fake:in", "3", "1", "0", "north_up"}, {"fake:out", "3", "1", "8", "south_up"},
                {"fake:side", "0", "1", "4", "west_up"}});
        StructureTemplate entrance = box("fake_entrance", 11, 8, 11, "fake:down", 5, 0, 5, "down_south");
        StructureTemplate castle = withJigsaws("fake_castle", 31, 12, 31,
            new String[][]{{"fake:down", "15", "0", "15", "down_north"}, {"fake:gate", "0", "1", "15", "west_up"}});
        return List.of(
            new MythicExtraPiece("fake_room", room, Role.SIDE_ROOM, "fake:entry", null, 6),
            new MythicExtraPiece("fake_corridor", corridor, Role.CORRIDOR, "fake:in", "fake:out", 40),
            new MythicExtraPiece("fake_entrance", entrance, Role.SURFACE_ENTRANCE, "fake:down", null, 4),
            new MythicExtraPiece("fake_castle", castle, Role.FULL_DUNGEON, "fake:down", null, 4));
    }

    private static StructureTemplate box(String name, int sx, int sy, int sz, String jigsaw, int x, int y, int z, String orientation) {
        return withJigsaws(name, sx, sy, sz, new String[][]{{jigsaw, "" + x, "" + y, "" + z, orientation}});
    }

    /** A hollow stone box with jigsaws carrying foreign pool names, as a third-party template would. */
    private static StructureTemplate withJigsaws(String name, int sx, int sy, int sz, String[][] jigsaws) {
        MythicCanvas canvas = new MythicCanvas(NamespacedId.of("fake", name), MythicDungeonTheme.GREEK, sx, sy, sz);
        canvas.fill(0, 0, 0, sx - 1, sy - 1, sz - 1, BlockStateSpec.of("minecraft:stone"));
        canvas.air(1, 1, 1, sx - 2, sy - 2, sz - 2);
        for (String[] jigsaw : jigsaws) {
            canvas.jigsaw(Integer.parseInt(jigsaw[1]), Integer.parseInt(jigsaw[2]), Integer.parseInt(jigsaw[3]), jigsaw[4],
                NamespacedId.parse(jigsaw[0]), NamespacedId.of("othermod", "target"), NamespacedId.of("othermod", "pool"),
                MythicCanvas.AIR, 0);
        }
        return canvas.build();
    }
}
