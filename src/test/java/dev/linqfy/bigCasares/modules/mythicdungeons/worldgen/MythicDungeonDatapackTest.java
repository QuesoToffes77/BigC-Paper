package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.linqfy.bigCasares.dimension.datapack.GeneratedDatapack;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.CompoundTag;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.IntTag;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.ListTag;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.Tag;
import dev.linqfy.bigCasares.dimension.worldgen.StructureTemplate;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicTemplateRewriter.Jigsaw;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MythicDungeonDatapackTest {

    /** Vanilla chest loot tables the pieces may reference (all exist in 26.2). */
    private static final Set<String> VANILLA_LOOT = Set.of(
        "minecraft:chests/simple_dungeon", "minecraft:chests/abandoned_mineshaft", "minecraft:chests/stronghold_corridor",
        "minecraft:chests/stronghold_crossing", "minecraft:chests/stronghold_library", "minecraft:chests/desert_pyramid",
        "minecraft:chests/jungle_temple");
    private static final Set<String> VANILLA_HOSTILES = Set.of(
        "minecraft:zombie", "minecraft:skeleton", "minecraft:husk", "minecraft:stray", "minecraft:spider");
    private static final int MIN_SPAWNERS = 8;

    private static GeneratedDatapack pack;
    private static MythicPackIndex index;

    @BeforeAll
    static void build() {
        pack = MythicDungeonDatapack.build();
        index = new MythicPackIndex(pack);
    }

    /** The decoded pack is large; the whole suite shares one test JVM, so do not keep it alive. */
    @AfterAll
    static void release() {
        pack = null;
        index = null;
    }

    @Test
    void buildIsDeterministic() {
        assertEquals(pack.contentHash(), MythicDungeonDatapack.build().contentHash());
        assertEquals(pack.contentHash(), MythicDungeonDatapack.build(List.of()).contentHash());
        assertTrue(pack.paths().contains("pack.mcmeta"));
    }

    @Test
    void everyCultureHasStructureSetTagAndPools() {
        Set<Integer> salts = new HashSet<>();
        for (MythicDungeonTheme theme : MythicDungeonTheme.all()) {
            String id = theme.structureId().asString();
            JsonObject structure = index.json(MythicPackIndex.jsonPath("worldgen/structure", id)).getAsJsonObject();
            assertEquals("minecraft:jigsaw", structure.get("type").getAsString());
            assertEquals("#bigc_myth:has_structure/" + theme.culture(), structure.get("biomes").getAsString());
            assertTrue(structure.get("size").getAsInt() >= 1 && structure.get("size").getAsInt() <= 20);
            // vanilla refuses the whole pack (the server does not start) above 128 including the adaptation margin
            int adaptationMargin = "none".equals(structure.get("terrain_adaptation").getAsString()) ? 0 : 12;
            assertTrue(structure.get("max_distance_from_center").getAsInt() + adaptationMargin <= 128);
            assertEquals("WORLD_SURFACE_WG", structure.get("project_start_to_heightmap").getAsString());
            assertTrue(index.pools.containsKey(structure.get("start_pool").getAsString()));

            JsonObject set = index.json(MythicPackIndex.jsonPath("worldgen/structure_set", id)).getAsJsonObject();
            assertEquals(id, set.getAsJsonArray("structures").get(0).getAsJsonObject().get("structure").getAsString());
            JsonObject placement = set.getAsJsonObject("placement");
            int spacing = placement.get("spacing").getAsInt();
            assertTrue(spacing >= 40 && spacing <= 60, theme.culture() + " spacing " + spacing);
            assertTrue(salts.add(placement.get("salt").getAsInt()), "salts must differ");

            JsonObject tag = index.json(MythicPackIndex.jsonPath("tags/worldgen/biome", "bigc_myth:has_structure/" + theme.culture()))
                .getAsJsonObject();
            assertFalse(tag.getAsJsonArray("values").isEmpty());
            for (JsonElement biome : tag.getAsJsonArray("values")) {
                assertTrue(isVanillaBiome(biome.getAsString()), "unknown biome " + biome);
            }
        }
    }

    @Test
    void poolsTemplatesAndJigsawsResolve() {
        for (MythicPackIndex.Pool pool : index.pools.values()) {
            assertFalse(pool.elements().isEmpty(), pool.id());
            for (MythicPackIndex.Element element : pool.elements()) {
                assertNotNull(index.templates.get(element.location()), "missing template " + element.location());
            }
            assertTrue(pool.fallback().equals("minecraft:empty") || index.pools.containsKey(pool.fallback()), pool.fallback());
        }
        for (var entry : index.templates.entrySet()) {
            for (Jigsaw jigsaw : MythicTemplateRewriter.jigsaws(entry.getValue())) {
                if (jigsaw.pool().equals("minecraft:empty")) {
                    assertEquals("minecraft:empty", jigsaw.target(), entry.getKey());
                    continue;
                }
                assertTrue(index.pools.containsKey(jigsaw.pool()), entry.getKey() + " -> " + jigsaw.pool());
                MythicPackIndex.Pool pool = index.pools.get(jigsaw.pool());
                List<StructureTemplate> candidates = new ArrayList<>(index.poolTemplates(pool.id()));
                candidates.addAll(index.poolTemplates(pool.fallback()));
                for (StructureTemplate candidate : candidates) {
                    assertTrue(MythicTemplateRewriter.jigsaws(candidate).stream().anyMatch(child -> child.name().equals(jigsaw.target())),
                        candidate.id() + " has no jigsaw named " + jigsaw.target() + " for " + entry.getKey());
                }
            }
        }
    }

    @Test
    void templatesUseOnlyVanillaBlocksAndValidBlockEntities() {
        for (var entry : index.templates.entrySet()) {
            StructureTemplate template = entry.getValue();
            int[] size = MythicTemplateRewriter.size(template);
            for (int axis : size) {
                assertTrue(axis >= 1 && axis <= 48, entry.getKey());
            }
            assertEquals(List.of(), MythicTemplateRewriter.nonVanillaIds(template), entry.getKey());
            for (String block : template.paletteBlocks()) {
                assertTrue(Material.matchMaterial(block) != null && declaresConstant("org.bukkit.block.BlockType", block),
                    entry.getKey() + " uses unknown block " + block);
            }
            for (Tag tag : template.root().getList("blocks").values()) {
                CompoundTag nbt = ((CompoundTag) tag).getCompound("nbt");
                if (nbt == null) {
                    continue;
                }
                switch (nbt.getString("id")) {
                    case "minecraft:chest" -> assertTrue(VANILLA_LOOT.contains(nbt.getString("LootTable")), nbt.getString("LootTable"));
                    case "minecraft:mob_spawner" -> {
                        String mob = nbt.getCompound("SpawnData").getCompound("entity").getString("id");
                        assertTrue(VANILLA_HOSTILES.contains(mob), mob);
                        assertNotNull(nbt.getList("SpawnPotentials"));
                    }
                    case "minecraft:jigsaw" -> assertNotNull(nbt.getString("final_state"));
                    default -> throw new AssertionError("Unexpected block entity " + nbt.getString("id"));
                }
            }
        }
    }

    @Test
    void eachCultureHasOneAltarInAnOpenArenaAndEnoughSpawners() {
        for (MythicDungeonTheme theme : MythicDungeonTheme.all()) {
            int lodestones = 0;
            int spawners = 0;
            int chests = 0;
            for (StructureTemplate template : index.cultureTemplates(theme.culture())) {
                Grid grid = new Grid(template);
                int here = grid.count("minecraft:lodestone");
                lodestones += here;
                if (here > 0) {
                    assertTrue(template.id().path().endsWith("/pieces/arena"), "lodestone outside arena: " + template.id());
                }
                spawners += grid.count("minecraft:spawner");
                chests += grid.count("minecraft:chest");
            }
            assertEquals(1, lodestones, theme.culture());
            assertTrue(spawners >= MIN_SPAWNERS, theme.culture() + " spawners " + spawners);
            assertTrue(chests >= 4, theme.culture() + " chests " + chests);

            Grid arena = new Grid(index.templates.get("bigc_myth:" + theme.culture() + "/pieces/arena"));
            assertTrue(arena.largestOpenSquare(10) >= 27, theme.culture() + " arena open square " + arena.largestOpenSquare(10));
            int altarX = MythicPieces.ALTAR_X;
            int altarZ = MythicPieces.ALTAR_Z;
            assertEquals("minecraft:lodestone", arena.name(altarX, 1, altarZ));
            assertTrue(arena.solid(altarX, 0, altarZ), "altar stands on the floor");
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx != 0 || dz != 0) {
                        assertEquals("minecraft:air", arena.name(altarX + dx, 1, altarZ + dz), "free space around altar");
                    }
                }
            }
            assertTrue(arena.watertightBelow(MythicPieces.ARENA_ENTRY_STAND), theme.culture() + " arena shell has holes");
        }
    }

    private static boolean isVanillaBiome(String id) {
        return declaresConstant("org.bukkit.block.Biome", id);
    }

    /**
     * True when the API type declares a constant for the vanilla id. Read reflectively without
     * initialising the class, which would need a live server registry.
     */
    private static boolean declaresConstant(String typeName, String id) {
        if (!id.startsWith("minecraft:")) {
            return false;
        }
        String field = id.substring("minecraft:".length()).toUpperCase(Locale.ROOT);
        try {
            Class<?> type = Class.forName(typeName, false, MythicDungeonDatapackTest.class.getClassLoader());
            Field declared = type.getDeclaredField(field);
            return type.isAssignableFrom(declared.getType());
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    /** Dense view of a template's blocks; unset cells are null. */
    static final class Grid {
        final int sizeX;
        final int sizeY;
        final int sizeZ;
        private final String[][][] names;

        Grid(StructureTemplate template) {
            int[] size = MythicTemplateRewriter.size(template);
            sizeX = size[0];
            sizeY = size[1];
            sizeZ = size[2];
            names = new String[sizeX][sizeY][sizeZ];
            List<Tag> palette = MythicTemplateRewriter.firstPalette(template.root()).values();
            for (Tag tag : template.root().getList("blocks").values()) {
                CompoundTag block = (CompoundTag) tag;
                ListTag pos = block.getList("pos");
                int state = ((IntTag) block.get("state")).value();
                names[value(pos, 0)][value(pos, 1)][value(pos, 2)] = ((CompoundTag) palette.get(state)).getString("Name");
            }
        }

        private static int value(ListTag list, int index) {
            return ((IntTag) list.values().get(index)).value();
        }

        String name(int x, int y, int z) {
            return names[x][y][z];
        }

        boolean solid(int x, int y, int z) {
            String name = names[x][y][z];
            return name != null && !name.equals("minecraft:air");
        }

        int count(String block) {
            int count = 0;
            for (String[][] plane : names) {
                for (String[] column : plane) {
                    for (String name : column) {
                        if (block.equals(name)) {
                            count++;
                        }
                    }
                }
            }
            return count;
        }

        /** Largest square of cells with solid floor at y=0 and air for y=1..headroom (altar allowed). */
        int largestOpenSquare(int headroom) {
            int[][] best = new int[sizeX][sizeZ];
            int result = 0;
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    boolean open = solid(x, 0, z);
                    for (int y = 1; open && y <= headroom; y++) {
                        String name = names[x][y][z];
                        open = "minecraft:air".equals(name) || (y == 1 && "minecraft:lodestone".equals(name));
                    }
                    if (open) {
                        best[x][z] = x == 0 || z == 0 ? 1
                            : 1 + Math.min(best[x - 1][z], Math.min(best[x][z - 1], best[x - 1][z - 1]));
                        result = Math.max(result, best[x][z]);
                    }
                }
            }
            return result;
        }

        /** Floor, walls and ceiling are solid except at or above the raised entrance level. */
        boolean watertightBelow(int entranceStand) {
            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    for (int z = 0; z < sizeZ; z++) {
                        boolean shell = x == 0 || y == 0 || z == 0 || x == sizeX - 1 || y == sizeY - 1 || z == sizeZ - 1;
                        if (shell && !solid(x, y, z) && !(z == 0 && y >= entranceStand)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }
}
