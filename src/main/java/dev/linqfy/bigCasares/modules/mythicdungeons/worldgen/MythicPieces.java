package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import dev.linqfy.bigCasares.dimension.NamespacedId;
import dev.linqfy.bigCasares.dimension.worldgen.BlockStateSpec;
import dev.linqfy.bigCasares.dimension.worldgen.StructureTemplate;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicCanvas.Face;

/**
 * Underground pieces shared by every culture. All are authored facing the same way:
 * the piece is entered through its north face (z = 0) and the main chain continues
 * through its south face, so the chain is a straight line that cannot fold onto itself.
 *
 * <p>Jigsaw contract: every doorway is 3 wide and 4 tall with its jigsaw at the
 * bottom-centre cell. A child's entry jigsaw is named {@code <culture>/door} (pool
 * and target {@code minecraft:empty}); parent openings target that name. Side doorways
 * may only receive side rooms that are at most 9 blocks wide, which keeps them inside
 * the parent's span along the chain and away from the next main piece. The main exit
 * has selection priority 1, so it is placed before any side room.</p>
 */
final class MythicPieces {

    static final NamespacedId EMPTY = NamespacedId.minecraft("empty");
    /** Walking level of every horizontal doorway, one above the floor layer. */
    static final int STAND = 1;

    enum RoomVariant { CRYPT, SHRINE, PILLARED, LIBRARY }

    enum SideVariant { TREASURY, DEN, OSSUARY }

    private MythicPieces() {
    }

    static NamespacedId door(MythicDungeonTheme theme) {
        return theme.id("door");
    }

    static NamespacedId shaftTop(MythicDungeonTheme theme) {
        return theme.id("shaft_top");
    }

    static NamespacedId template(MythicDungeonTheme theme, String name) {
        return theme.id("pieces/" + name);
    }

    private static void entry(MythicCanvas canvas, MythicDungeonTheme theme, int x, int y) {
        canvas.doorway(Face.NORTH, x, y, 0);
        canvas.jigsaw(x, y, 0, Face.NORTH.orientation(), door(theme), EMPTY, EMPTY, MythicCanvas.AIR, 0);
    }

    private static void exit(MythicCanvas canvas, MythicDungeonTheme theme, int x, int y, NamespacedId next) {
        int z = canvas.sizeZ() - 1;
        canvas.doorway(Face.SOUTH, x, y, z);
        canvas.jigsaw(x, y, z, Face.SOUTH.orientation(), EMPTY, door(theme), next, MythicCanvas.AIR, 1);
    }

    private static void sideDoor(MythicCanvas canvas, MythicDungeonTheme theme, Face face, int z, NamespacedId sidePool) {
        int x = face == Face.WEST ? 0 : canvas.sizeX() - 1;
        canvas.doorway(face, x, STAND, z);
        canvas.jigsaw(x, STAND, z, face.orientation(), EMPTY, door(theme), sidePool, MythicCanvas.AIR, 0);
    }

    /** 5x6x1 solid plug used as fallback for any doorway that could not get a piece. */
    static StructureTemplate seal(MythicDungeonTheme theme) {
        MythicCanvas canvas = new MythicCanvas(template(theme, "seal"), theme, 5, 6, 1);
        canvas.wall(0, 0, 0, 4, 5, 0);
        canvas.jigsaw(2, 1, 0, Face.NORTH.orientation(), door(theme), EMPTY, EMPTY, theme.palette().wall(), 0);
        return canvas.build();
    }

    /**
     * Straight stair under the entrance: entered from above through a 3x3 opening in its
     * top layer (vertical jigsaw), descends 12 blocks southwards to a doorway.
     */
    static StructureTemplate shaft(MythicDungeonTheme theme, String name, NamespacedId next) {
        MythicCanvas canvas = new MythicCanvas(template(theme, name), theme, 7, 13, 15);
        MythicDungeonTheme.Palette palette = theme.palette();
        canvas.room(0, 0, 0, 6, 12, 14);
        canvas.air(2, 12, 1, 4, 12, 3);
        for (int z = 1; z <= 12; z++) {
            int top = 13 - z;
            int fromX = z == 1 ? 2 : 1;
            int toX = z == 1 ? 4 : 5;
            for (int x = fromX; x <= toX; x++) {
                if (top > 1) {
                    canvas.wall(x, 1, z, x, top - 1, z);
                }
                canvas.set(x, top, z, palette.stairs("north", false));
            }
            if (z == 1) {
                canvas.wall(1, 1, 1, 1, 11, 1);
                canvas.wall(5, 1, 1, 5, 11, 1);
            }
            if (z % 3 == 0) {
                int lightY = Math.min(11, top + 2);
                canvas.set(0, lightY, z, palette.light());
                canvas.set(6, lightY, z, palette.light());
            }
        }
        canvas.jigsaw(3, 12, 2, "up_south", shaftTop(theme), EMPTY, EMPTY, MythicCanvas.AIR, 0);
        exit(canvas, theme, 3, STAND, next);
        return canvas.build();
    }

    /** 9x7x11 corridor with pillars, one spawner and two side doorways. */
    static StructureTemplate hall(MythicDungeonTheme theme, String name, NamespacedId next, NamespacedId sidePool) {
        MythicCanvas canvas = new MythicCanvas(template(theme, name), theme, 9, 7, 11);
        MythicDungeonTheme.Palette palette = theme.palette();
        canvas.room(0, 0, 0, 8, 6, 10);
        for (int z : new int[]{2, 8}) {
            canvas.pillar(1, 1, z, 5);
            canvas.pillar(7, 1, z, 5);
        }
        canvas.set(4, 6, 3, palette.light()).set(4, 6, 7, palette.light());
        canvas.set(4, 5, 5, palette.hangingLight());
        canvas.spawner(7, 1, 9, theme.spawnerMob());
        canvas.set(1, 1, 9, palette.clutter()).set(1, 1, 1, palette.clutter());
        entry(canvas, theme, 4, STAND);
        sideDoor(canvas, theme, Face.WEST, 5, sidePool);
        sideDoor(canvas, theme, Face.EAST, 5, sidePool);
        exit(canvas, theme, 4, STAND, next);
        return canvas.build();
    }

    /** 15x9x15 main room; the variant decides furniture, spawners and chests. */
    static StructureTemplate room(MythicDungeonTheme theme, String name, RoomVariant variant,
                                  NamespacedId next, NamespacedId sidePool) {
        MythicCanvas canvas = new MythicCanvas(template(theme, name), theme, 15, 9, 15);
        MythicDungeonTheme.Palette palette = theme.palette();
        canvas.room(0, 0, 0, 14, 8, 14);
        canvas.ceilingLights(3, 3, 11, 11, 8, 8);
        canvas.set(7, 8, 7, palette.light());
        for (int x = 1; x <= 13; x++) {
            canvas.set(x, 0, 7, palette.accent2());
        }
        switch (variant) {
            case CRYPT -> {
                for (int[] tomb : new int[][]{{2, 2}, {10, 2}, {2, 11}, {10, 11}}) {
                    for (int x = tomb[0]; x <= tomb[0] + 2; x++) {
                        for (int z = tomb[1]; z <= tomb[1] + 1; z++) {
                            canvas.set(x, 1, z, palette.accent2());
                            canvas.set(x, 2, z, palette.slab("bottom"));
                        }
                    }
                }
                canvas.spawner(11, 1, 5, theme.spawnerMob());
                canvas.spawner(3, 1, 9, theme.spawnerMob());
                canvas.chest(12, 1, 9, "west", theme.commonLoot());
                canvas.set(1, 1, 1, palette.clutter()).set(13, 1, 13, palette.clutter());
                canvas.set(13, 1, 1, palette.clutter()).set(1, 1, 13, palette.clutter());
                canvas.set(7, 7, 3, palette.hangingLight()).set(7, 7, 11, palette.hangingLight());
            }
            case SHRINE -> {
                for (int x = 5; x <= 9; x++) {
                    for (int z = 5; z <= 9; z++) {
                        canvas.set(x, 1, z, palette.slab("bottom"));
                    }
                }
                for (int[] corner : new int[][]{{5, 5}, {9, 5}, {5, 9}, {9, 9}}) {
                    canvas.pillar(corner[0], 1, corner[1], 7);
                }
                canvas.set(7, 1, 7, palette.accent());
                canvas.set(7, 2, 7, palette.clutter());
                canvas.spawner(2, 1, 2, theme.spawnerMob());
                canvas.spawner(12, 1, 12, theme.spawnerMob());
                canvas.chest(12, 1, 2, "west", theme.commonLoot());
                canvas.chest(2, 1, 12, "east", theme.commonLoot());
            }
            case PILLARED -> {
                for (int x : new int[]{4, 10}) {
                    for (int z : new int[]{2, 4, 10, 12}) {
                        canvas.pillar(x, 1, z, 7);
                    }
                }
                canvas.spawner(2, 1, 4, theme.spawnerMob());
                canvas.spawner(12, 1, 10, theme.spawnerMob());
                canvas.chest(2, 1, 12, "east", theme.commonLoot());
                canvas.set(12, 1, 2, palette.clutter()).set(12, 1, 12, palette.clutter());
                canvas.set(7, 7, 4, palette.hangingLight()).set(7, 7, 10, palette.hangingLight());
            }
            case LIBRARY -> {
                for (int x : new int[]{1, 13}) {
                    for (int z = 1; z <= 13; z++) {
                        if (z < 5 || z > 9) {
                            canvas.fill(x, 1, z, x, 3, z, BlockStateSpec.of("minecraft:bookshelf"));
                        }
                    }
                }
                canvas.set(4, 1, 11, palette.accent2()).set(10, 1, 11, palette.accent2());
                canvas.chest(4, 1, 3, "south", theme.commonLoot());
                canvas.chest(10, 1, 3, "south", theme.treasureLoot());
                canvas.spawner(4, 1, 10, theme.spawnerMob());
                canvas.spawner(10, 1, 10, theme.spawnerMob());
                canvas.set(7, 7, 3, palette.hangingLight()).set(7, 7, 11, palette.hangingLight());
            }
        }
        entry(canvas, theme, 7, STAND);
        sideDoor(canvas, theme, Face.WEST, 7, sidePool);
        sideDoor(canvas, theme, Face.EAST, 7, sidePool);
        exit(canvas, theme, 7, STAND, next);
        return canvas.build();
    }

    /** 7x14x12 grand stair: enters at walking level 9, leaves 8 blocks lower. */
    static StructureTemplate stairs(MythicDungeonTheme theme, String name, NamespacedId next) {
        MythicCanvas canvas = new MythicCanvas(template(theme, name), theme, 7, 14, 12);
        MythicDungeonTheme.Palette palette = theme.palette();
        canvas.room(0, 0, 0, 6, 13, 11);
        canvas.wall(1, 1, 1, 5, 7, 1);
        canvas.fill(1, 8, 1, 5, 8, 1, palette.floor());
        for (int z = 2; z <= 8; z++) {
            int top = 9 - z;
            if (top > 1) {
                canvas.wall(1, 1, z, 5, top - 1, z);
            }
            for (int x = 1; x <= 5; x++) {
                canvas.set(x, top, z, palette.stairs("north", false));
            }
        }
        for (int z : new int[]{2, 5, 8}) {
            int lightY = 9 - z + 3;
            canvas.set(0, lightY, z, palette.light()).set(6, lightY, z, palette.light());
        }
        entry(canvas, theme, 3, 9);
        exit(canvas, theme, 3, STAND, next);
        return canvas.build();
    }

    /** Arena size and altar position; tests and runtime-facing checks use these. */
    static final int ARENA_SIZE = 35;
    static final int ARENA_HEIGHT = 18;
    static final int ALTAR_X = 17;
    static final int ALTAR_Z = 20;
    /** The arena doorway is raised so ~2 blocks of flood water stay inside. */
    static final int ARENA_ENTRY_STAND = 4;

    /**
     * Boss arena: 33x33 interior with 16 blocks of headroom, watertight shell, raised
     * entrance with a short stair, and the lodestone altar on the floor near the centre.
     */
    static StructureTemplate arena(MythicDungeonTheme theme) {
        int max = ARENA_SIZE - 1;
        int top = ARENA_HEIGHT - 1;
        MythicCanvas canvas = new MythicCanvas(template(theme, "arena"), theme, ARENA_SIZE, ARENA_HEIGHT, ARENA_SIZE);
        MythicDungeonTheme.Palette palette = theme.palette();
        canvas.room(0, 0, 0, max, top, max);
        for (int x = 1; x < max; x++) {
            for (int z = 1; z < max; z++) {
                double distance = Math.hypot(x - ALTAR_X, z - ALTAR_Z);
                if ((distance >= 5.5 && distance < 6.5) || (distance >= 10.5 && distance < 11.5)) {
                    canvas.set(x, 0, z, palette.accent());
                } else if (distance < 2.5) {
                    canvas.set(x, 0, z, palette.accent2());
                }
            }
        }
        for (int i = 0; i <= max; i++) {
            canvas.set(i, 12, 0, palette.accent2()).set(i, 12, max, palette.accent2());
            canvas.set(0, 12, i, palette.accent2()).set(max, 12, i, palette.accent2());
        }
        for (int i = 3; i < max; i += 6) {
            canvas.set(i, 6, max, palette.light()).set(0, 6, i, palette.light()).set(max, 6, i, palette.light());
        }
        canvas.ceilingLights(2, 2, 32, 32, top, 5);
        for (int z = 8; z < max; z += 6) {
            canvas.pillar(1, 1, z, top - 1);
            canvas.pillar(max - 1, 1, z, top - 1);
        }
        int door = ALTAR_X;
        canvas.wall(door - 2, 1, 1, door + 2, 2, 3);
        canvas.fill(door - 2, 3, 1, door + 2, 3, 3, palette.floor());
        for (int x = door - 2; x <= door + 2; x++) {
            canvas.set(x, 1, 4, palette.wall());
            canvas.set(x, 2, 4, palette.stairs("north", false));
            canvas.set(x, 1, 5, palette.stairs("north", false));
        }
        canvas.set(ALTAR_X, 1, ALTAR_Z, BlockStateSpec.of("minecraft:lodestone"));
        entry(canvas, theme, door, ARENA_ENTRY_STAND);
        return canvas.build();
    }

    /** Dead-end side rooms (9 wide), entered from the north. */
    static StructureTemplate sideRoom(MythicDungeonTheme theme, SideVariant variant) {
        int depth = variant == SideVariant.DEN ? 11 : 9;
        MythicCanvas canvas = new MythicCanvas(template(theme, "side_" + variant.name().toLowerCase(java.util.Locale.ROOT)),
            theme, 9, 7, depth);
        MythicDungeonTheme.Palette palette = theme.palette();
        canvas.room(0, 0, 0, 8, 6, depth - 1);
        canvas.set(4, 6, depth / 2, palette.light());
        switch (variant) {
            case TREASURY -> {
                for (int x = 2; x <= 6; x += 2) {
                    canvas.chest(x, 1, 7, "north", x == 4 ? theme.treasureLoot() : theme.commonLoot());
                    canvas.set(x, 0, 7, palette.accent());
                }
                canvas.spawner(4, 1, 4, theme.spawnerMob());
                canvas.pillar(1, 1, 7, 5);
                canvas.pillar(7, 1, 7, 5);
            }
            case DEN -> {
                canvas.spawner(2, 1, 5, theme.spawnerMob());
                canvas.spawner(6, 1, 8, theme.spawnerMob());
                canvas.chest(4, 1, 9, "north", theme.commonLoot());
                canvas.set(1, 1, 1, palette.clutter()).set(7, 1, 1, palette.clutter());
                canvas.set(1, 1, 9, palette.clutter()).set(7, 1, 9, palette.clutter());
            }
            case OSSUARY -> {
                for (int z = 2; z <= 6; z += 2) {
                    canvas.set(1, 1, z, palette.clutter()).set(7, 1, z, palette.clutter());
                    canvas.set(1, 2, z, palette.accent2()).set(7, 2, z, palette.accent2());
                }
                canvas.spawner(4, 1, 5, theme.spawnerMob());
                canvas.chest(1, 1, 7, "east", theme.commonLoot());
                canvas.set(4, 5, 3, palette.hangingLight());
            }
        }
        entry(canvas, theme, 4, STAND);
        return canvas.build();
    }
}
