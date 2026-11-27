package dev.linqfy.bigCasares.modules.duels;

import org.bukkit.Material;

/** Incremental traversal of the largest arena footprint. */
final class DuelArenaWork {
    @FunctionalInterface interface BlockAction { void apply(int x, int y, int z); }
    @FunctionalInterface interface BuildAction { void apply(int x, int y, int z, Material material); }
    private static final int WIDTH = 69;
    private static final int HEIGHT = 17;
    private static final int TOTAL = WIDTH * WIDTH * HEIGHT;
    private int cursor;

    boolean done() { return cursor == TOTAL; }

    void step(int budget, BlockAction action) {
        int end = cursor + Math.min(Math.max(0, budget), TOTAL - cursor);
        while (cursor < end) {
            int index = cursor;
            int y = index % HEIGHT + 62;
            int z = index / HEIGHT % WIDTH - 34;
            int x = index / (HEIGHT * WIDTH) - 34;
            action.apply(x, y, z);
            cursor++;
        }
    }

    /** Traverses the floor followed by each wall layer, using the same bounds as the arena geometry. */
    static final class Build {
        private final int half;
        private final int side;
        private final int floorBlocks;
        private final int total;
        private int cursor;

        Build(DuelArenaSize size) {
            half = (int) Math.ceil(size.halfSide());
            side = half * 2;
            floorBlocks = side * side;
            total = floorBlocks + (DuelArenaWorld.WALL_HEIGHT + 1) * (side + 2) * 4;
        }

        boolean done() { return cursor == total; }

        void step(int budget, BuildAction action) {
            int end = cursor + Math.min(Math.max(0, budget), total - cursor);
            while (cursor < end) {
                if (cursor < floorBlocks) {
                    int x = cursor / side - half;
                    int z = cursor % side - half;
                    boolean border = x == -half || z == -half || x == half - 1 || z == half - 1;
                    boolean stripe = x == 0 || x == -1;
                    Material floor = border ? Material.POLISHED_BLACKSTONE_BRICKS
                        : stripe ? Material.RED_CONCRETE : Material.STONE_BRICKS;
                    action.apply(x, DuelArenaWorld.FLOOR_Y, z, floor);
                } else {
                    int index = cursor - floorBlocks;
                    int perLayer = (side + 2) * 4;
                    int y = DuelArenaWorld.FLOOR_Y + index / perLayer;
                    int i = index % perLayer / 4 - half - 1;
                    switch (index % 4) {
                        case 0 -> action.apply(i, y, -half - 1, Material.BARRIER);
                        case 1 -> action.apply(i, y, half, Material.BARRIER);
                        case 2 -> action.apply(-half - 1, y, i, Material.BARRIER);
                        case 3 -> action.apply(half, y, i, Material.BARRIER);
                        default -> throw new AssertionError();
                    }
                }
                cursor++;
            }
        }
    }
}
