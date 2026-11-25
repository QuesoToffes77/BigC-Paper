package dev.linqfy.bigCasares.modules.toffy.eventos;

import org.bukkit.Material;

/** Computes arena blocks without allocating a list proportional to the volume. */
final class ArenaBuildPlan {
    private final ArenaLayout layout;

    ArenaBuildPlan(ArenaLayout layout) {
        this.layout = layout;
    }

    int maxY() {
        return layout.type() == ToffyEventType.SPLEEF
            ? Math.max(layout.clearMaxY(), layout.floorY() + layout.layerGap() - 2)
            : layout.clearMaxY();
    }

    Material material(int x, int y, int z) {
        int dx = x - layout.centerX();
        int dz = z - layout.centerZ();
        int radius = layout.radius();
        boolean inner = ArenaLayout.inDisc(dx, dz, radius);
        switch (layout.type()) {
            case KOTH -> {
                if (inner && y == layout.floorY() - 1) {
                    return ArenaLayout.inDisc(dx, dz, radius - 1) ? Material.SMOOTH_STONE : Material.POLISHED_BLACKSTONE;
                }
                if (inner && y == layout.floorY() && ArenaLayout.inDisc(dx, dz, layout.hillRadius() + 2)) {
                    return ArenaLayout.inDisc(dx, dz, layout.hillRadius()) ? Material.GOLD_BLOCK : Material.STONE_BRICKS;
                }
            }
            case TNT_TAG -> {
                if (y == layout.floorY() - 1 && inner) {
                    return ((dx + radius) / 3 + (dz + radius) / 3) % 2 == 0
                        ? Material.RED_CONCRETE : Material.WHITE_CONCRETE;
                }
                if (!inner && ArenaLayout.inDisc(dx, dz, radius + 1)) {
                    if (y == layout.floorY() - 1) return Material.BLACK_CONCRETE;
                    if (y >= layout.floorY() && y < layout.floorY() + 4) return Material.BARRIER;
                }
            }
            case SPLEEF -> {
                for (int layer = 0; layer < layout.layers(); layer++) {
                    int floor = layout.floorY() - 1 - layer * layout.layerGap();
                    if (inner && y == floor) return Material.SNOW_BLOCK;
                    if (!inner && ArenaLayout.inDisc(dx, dz, radius + 1)
                        && y >= floor && y < floor + layout.layerGap()) return Material.BARRIER;
                }
            }
            default -> throw new IllegalArgumentException("Not an arena: " + layout.type());
        }
        return y <= layout.clearMaxY() ? Material.AIR : null;
    }

    record Cell(int x, int y, int z) { }

    static final class Cursor {
        private final int xMin, xMax, zMin, zMax, yMin, yMax;
        private int x, y, z;

        Cursor(ArenaLayout layout, int chunkX, int chunkZ) {
            int radius = layout.clearRadius();
            xMin = Math.max(layout.centerX() - radius, chunkX << 4);
            xMax = Math.min(layout.centerX() + radius, (chunkX << 4) + 15);
            zMin = Math.max(layout.centerZ() - radius, chunkZ << 4);
            zMax = Math.min(layout.centerZ() + radius, (chunkZ << 4) + 15);
            yMin = layout.clearMinY();
            yMax = new ArenaBuildPlan(layout).maxY();
            x = xMin;
            z = zMin;
            y = yMin;
        }

        boolean hasNext() { return x <= xMax && zMin <= zMax; }

        Cell next() {
            if (!hasNext()) throw new java.util.NoSuchElementException();
            Cell cell = new Cell(x, y, z);
            if (++y > yMax) {
                y = yMin;
                if (++z > zMax) {
                    z = zMin;
                    x++;
                }
            }
            return cell;
        }
    }
}
