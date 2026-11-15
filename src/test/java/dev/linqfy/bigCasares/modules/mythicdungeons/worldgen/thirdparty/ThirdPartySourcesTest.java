package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.thirdparty;

import dev.linqfy.bigCasares.dimension.datapack.GeneratedDatapack;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.CompoundTag;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.Tag;
import dev.linqfy.bigCasares.dimension.worldgen.BlockStateSpec;
import dev.linqfy.bigCasares.dimension.worldgen.StructureTemplate;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicDungeonDatapack;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicDungeonTheme;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicExtraPiece;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicTemplateSource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirdPartySourcesTest {

    private static final Set<String> VANILLA_LOOT = Set.of(
        "minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft", "minecraft:chests/stronghold_corridor",
        "minecraft:chests/stronghold_crossing", "minecraft:chests/stronghold_library", "minecraft:chests/desert_pyramid",
        "minecraft:chests/jungle_temple");
    private static final Set<String> VANILLA_HOSTILES = Set.of(
        "minecraft:zombie", "minecraft:skeleton", "minecraft:husk", "minecraft:stray", "minecraft:spider");
    private static final int SURFACE_Y = 62;
    private static final int WORLD_MIN_Y = -64;
    private static final int SEEDS = 60;

    /**
     * Pack whose extra pieces are limited to one culture: the decoded index of the full pack does not
     * fit the default test heap next to the other worldgen tests, so each culture is checked on its own.
     */
    private static PackHarness harnessFor(String culture) {
        List<MythicTemplateSource> sources = new ArrayList<>();
        for (MythicTemplateSource source : ThirdPartySources.all()) {
            sources.add(requested -> requested.equals(culture) ? source.piecesFor(requested) : List.of());
        }
        return new PackHarness(MythicDungeonDatapack.build(sources), MythicDungeonDatapack.JIGSAW_SIZE,
            MythicDungeonDatapack.MAX_DISTANCE_FROM_CENTER);
    }

    @Test
    void packIsDeterministicAndContainsBothSources() {
        GeneratedDatapack pack = MythicDungeonDatapack.build(ThirdPartySources.all());
        assertEquals(pack.contentHash(), MythicDungeonDatapack.build(ThirdPartySources.all()).contentHash());
        assertNotEquals(MythicDungeonDatapack.build().contentHash(), pack.contentHash());
        for (MythicTemplateSource source : ThirdPartySources.all()) {
            // The module's own validation: an invalid piece makes build() throw.
            MythicDungeonDatapack.build(List.of(source));
        }
        for (MythicDungeonTheme theme : MythicDungeonTheme.all()) {
            String prefix = "data/bigc_myth/structure/" + theme.culture() + "/extra/";
            List<String> extras = pack.paths().stream().filter(path -> path.startsWith(prefix)).toList();
            assertTrue(extras.stream().anyMatch(id -> id.contains("/yungs/")), theme.culture() + " " + extras);
            assertTrue(extras.stream().anyMatch(id -> id.contains("/doomlike/dungeon_")), theme.culture() + " " + extras);
            assertTrue(extras.stream().anyMatch(id -> id.contains("/doomlike/room_")), theme.culture() + " " + extras);
        }
    }

    private static void checkTemplates(PackHarness harness) {
        for (Map.Entry<String, StructureTemplate> entry : harness.templates().entrySet()) {
            StructureTemplate template = entry.getValue();
            for (int axis : PackHarness.size(template)) {
                assertTrue(axis >= 1 && axis <= 48, entry.getKey());
            }
            for (String block : template.paletteBlocks()) {
                assertTrue(declaresConstant(block), entry.getKey() + " uses unknown block " + block);
            }
            for (Tag tag : template.root().getList("blocks").values()) {
                CompoundTag nbt = ((CompoundTag) tag).getCompound("nbt");
                if (nbt == null) {
                    continue;
                }
                switch (nbt.getString("id")) {
                    case "minecraft:chest" -> assertTrue(VANILLA_LOOT.contains(nbt.getString("LootTable")),
                        entry.getKey() + " loot " + nbt.getString("LootTable"));
                    case "minecraft:mob_spawner" -> {
                        assertTrue(VANILLA_HOSTILES.contains(nbt.getCompound("SpawnData").getCompound("entity").getString("id")));
                        assertNotNull(nbt.getList("SpawnPotentials"));
                    }
                    case "minecraft:jigsaw" -> assertNotNull(nbt.getString("final_state"));
                    default -> throw new AssertionError(entry.getKey() + " has block entity " + nbt.getString("id"));
                }
            }
        }
    }

    /** The pack's reference check: pools, fallbacks and jigsaw targets all resolve. */
    private static void checkReferences(PackHarness harness) {
        for (String pool : harness.pools().keySet()) {
            List<String> elements = harness.poolElements(pool);
            assertFalse(elements.isEmpty(), pool);
            for (String element : elements) {
                assertNotNull(harness.templates().get(element), "missing template " + element);
            }
            String fallback = harness.poolFallback(pool);
            assertTrue(fallback.equals("minecraft:empty") || harness.pools().containsKey(fallback), fallback);
        }
        for (Map.Entry<String, StructureTemplate> entry : harness.templates().entrySet()) {
            for (PackHarness.Jigsaw jigsaw : PackHarness.jigsaws(entry.getValue())) {
                if (jigsaw.pool().equals("minecraft:empty")) {
                    assertEquals("minecraft:empty", jigsaw.target(), entry.getKey());
                    continue;
                }
                assertTrue(harness.pools().containsKey(jigsaw.pool()), entry.getKey() + " -> " + jigsaw.pool());
                List<String> candidates = new ArrayList<>(harness.poolElements(jigsaw.pool()));
                candidates.addAll(harness.poolElements(harness.poolFallback(jigsaw.pool())));
                for (String candidate : candidates) {
                    assertTrue(PackHarness.jigsaws(harness.templates().get(candidate)).stream()
                            .anyMatch(child -> child.name().equals(jigsaw.target())),
                        candidate + " has no jigsaw named " + jigsaw.target() + " for " + entry.getKey());
                }
            }
        }
    }

    /**
     * Per culture: vanilla-only templates with valid block entities, resolving references, and the
     * jigsaw simulation always reaching the arena (with its lodestone) without overlaps.
     */
    @Test
    void piecesValidateAndTheArenaIsAlwaysReached() {
        int fullDungeons = 0;
        int yungs = 0;
        int doomlikeRooms = 0;
        int corridors = 0;
        for (MythicDungeonTheme theme : MythicDungeonTheme.all()) {
            PackHarness harness = harnessFor(theme.culture());
            checkTemplates(harness);
            checkReferences(harness);
            String arenaId = "bigc_myth:" + theme.culture() + "/pieces/arena";
            assertTrue(harness.templates().get(arenaId).paletteBlocks().contains("minecraft:lodestone"), theme.culture());
            for (long seed = 0; seed < SEEDS; seed++) {
                List<PackHarness.Placed> pieces = harness.assemble(theme.id("start").asString(), seed, SURFACE_Y, WORLD_MIN_Y);
                List<String> paths = pieces.stream().map(PackHarness.Placed::path).toList();
                assertEquals(1, paths.stream().filter(path -> path.endsWith("/pieces/arena")).count(),
                    theme.culture() + " seed " + seed + " " + paths);
                for (int a = 0; a < pieces.size(); a++) {
                    for (int b = a + 1; b < pieces.size(); b++) {
                        assertFalse(PackHarness.intersects(pieces.get(a).box(), pieces.get(b).box()),
                            theme.culture() + " seed " + seed + " overlap " + pieces.get(a).path() + " / " + pieces.get(b).path());
                    }
                }
                fullDungeons += (int) paths.stream().filter(path -> path.contains("/extra/full_dungeon/")).count();
                yungs += (int) paths.stream().filter(path -> path.contains("/yungs/")).count();
                doomlikeRooms += (int) paths.stream().filter(path -> path.contains("/extra/side_room/doomlike/")).count();
                corridors += (int) paths.stream().filter(path -> path.contains("/extra/corridor_")).count();
            }
        }
        assertTrue(fullDungeons > 0 && yungs > 0 && doomlikeRooms > 0 && corridors > 0,
            "full " + fullDungeons + " yungs " + yungs + " doomlike rooms " + doomlikeRooms + " corridors " + corridors);
    }

    @Test
    void yungsPlaceholdersAreResolvedAndOriginalsShipUnmodified() throws Exception {
        YungsBetterDungeonsSource source = new YungsBetterDungeonsSource();
        for (YungsBetterDungeonsSource.Spec spec : YungsBetterDungeonsSource.ALL) {
            for (MythicDungeonTheme theme : MythicDungeonTheme.all()) {
                TemplateGrid grid = source.resolved(spec, theme);
                for (TemplateGrid.Cell cell : grid.cells()) {
                    assertFalse(YungsPlaceholders.PLACEHOLDERS.contains(cell.name()), spec.path() + " keeps " + cell.name());
                    if (cell.name().equals("minecraft:jigsaw")) {
                        assertTrue(cell.nbt().getString("name").startsWith("bigc_myth:third_party/"), spec.path());
                    }
                }
            }
            byte[] shipped = YungsBetterDungeonsSource.class
                .getResourceAsStream(YungsBetterDungeonsSource.RESOURCE_ROOT + spec.path() + ".nbt").readAllBytes();
            Path original = Path.of("work/mods/yungs/data/betterdungeons/structure", spec.path() + ".nbt");
            if (Files.exists(original)) {
                assertArrayEquals(Files.readAllBytes(original), shipped, spec.path());
            }
        }
        for (MythicDungeonTheme theme : MythicDungeonTheme.all()) {
            Set<String> paletteIds = paletteIds(theme);
            for (MythicExtraPiece piece : source.piecesFor(theme.culture())) {
                for (String block : piece.template().paletteBlocks()) {
                    assertTrue(!YungsPlaceholders.PLACEHOLDERS.contains(block) || paletteIds.contains(block),
                        theme.culture() + " " + piece.name() + " keeps placeholder " + block);
                }
            }
        }
    }

    @Test
    void creditsNameExactlyTheTwoMods() {
        assertEquals(List.of("YUNG's Better Dungeons", "Doomlike Dungeons"),
            ThirdPartyCredits.ALL.stream().map(ThirdPartyCredits.Credit::name).toList());
        assertEquals(List.of("LGPL-3.0", "MIT"), ThirdPartyCredits.ALL.stream().map(ThirdPartyCredits.Credit::license).toList());
        for (String path : List.of("yungs_better_dungeons/LICENSE", "yungs_better_dungeons/NOTICE",
            "doomlike_dungeons/LICENSE", "doomlike_dungeons/NOTICE")) {
            assertNotNull(ThirdPartyCredits.class.getResource("/mythicdungeons/third_party/" + path), path);
        }
    }

    private static Set<String> paletteIds(MythicDungeonTheme theme) {
        MythicDungeonTheme.Palette p = theme.palette();
        Set<String> ids = new HashSet<>();
        for (BlockStateSpec spec : List.of(p.wall(), p.wallAlt(), p.floor(), p.floorAlt(), p.ceiling(), p.pillar(), p.light(),
            p.accent(), p.accent2(), p.clutter(), p.hangingLight(), p.roof())) {
            ids.add(spec.block().asString());
        }
        ids.add(p.stairs());
        ids.add(p.slab());
        return ids;
    }

    /** True when Bukkit's BlockType declares the vanilla id (read without initialising the class). */
    static boolean declaresConstant(String id) {
        if (!id.startsWith("minecraft:")) {
            return false;
        }
        String field = id.substring("minecraft:".length()).toUpperCase(Locale.ROOT);
        try {
            Class<?> type = Class.forName("org.bukkit.block.BlockType", false, ThirdPartySourcesTest.class.getClassLoader());
            Field declared = type.getDeclaredField(field);
            return type.isAssignableFrom(declared.getType());
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
