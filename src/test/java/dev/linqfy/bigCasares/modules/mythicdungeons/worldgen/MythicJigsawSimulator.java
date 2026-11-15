package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import dev.linqfy.bigCasares.dimension.worldgen.StructureTemplate;
import dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.MythicTemplateRewriter.Jigsaw;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * In-memory model of vanilla {@code JigsawPlacement} for rigid pieces: breadth-first expansion,
 * shuffled weighted pool candidates followed by the fallback pool, all four rotations, jigsaw
 * matching by facing/top/name, and placement only inside the max-distance box without overlapping
 * any earlier piece.
 */
final class MythicJigsawSimulator {

    record Box(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        boolean intersects(Box other) {
            return minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
        }

        boolean within(Box other) {
            return minX >= other.minX && maxX <= other.maxX && minY >= other.minY && maxY <= other.maxY
                && minZ >= other.minZ && maxZ <= other.maxZ;
        }
    }

    record Placed(StructureTemplate template, int rotation, int originX, int originY, int originZ, Box box, int depth) {
    }

    private static final String[] HORIZONTAL = {"north", "east", "south", "west"};

    private final MythicPackIndex index;
    private final int maxDepth;
    private final int maxDistance;

    MythicJigsawSimulator(MythicPackIndex index, int maxDepth, int maxDistance) {
        this.index = index;
        this.maxDepth = maxDepth;
        this.maxDistance = maxDistance;
    }

    List<Placed> assemble(String startPool, long seed, int surfaceY, int worldMinY) {
        Random random = new Random(seed);
        StructureTemplate start = shuffled(startPool, random).getFirst();
        int rotation = random.nextInt(4);
        Placed first = place(start, rotation, 0, surfaceY, 0, 0);
        int centerX = (first.box().minX() + first.box().maxX()) / 2;
        int centerZ = (first.box().minZ() + first.box().maxZ()) / 2;
        Box limit = new Box(centerX - maxDistance, Math.max(worldMinY, surfaceY - maxDistance), centerZ - maxDistance,
            centerX + maxDistance, surfaceY + maxDistance, centerZ + maxDistance);
        List<Placed> placed = new ArrayList<>(List.of(first));
        Deque<Placed> queue = new ArrayDeque<>(List.of(first));
        while (!queue.isEmpty()) {
            Placed parent = queue.removeFirst();
            List<Jigsaw> jigsaws = new ArrayList<>(MythicTemplateRewriter.jigsaws(parent.template()));
            Collections.shuffle(jigsaws, random);
            jigsaws.sort(Comparator.comparingInt(MythicJigsawSimulator::priority).reversed());
            for (Jigsaw jigsaw : jigsaws) {
                if (jigsaw.pool().equals("minecraft:empty")) {
                    continue;
                }
                String front = rotate(jigsaw.front(), parent.rotation());
                String top = rotate(jigsaw.top(), parent.rotation());
                int[] world = worldPos(parent, jigsaw);
                int[] step = step(front);
                int targetX = world[0] + step[0];
                int targetY = world[1] + step[1];
                int targetZ = world[2] + step[2];
                List<StructureTemplate> candidates = new ArrayList<>();
                MythicPackIndex.Pool pool = index.pools.get(jigsaw.pool());
                if (parent.depth() != maxDepth) {
                    candidates.addAll(shuffled(pool.id(), random));
                }
                candidates.addAll(shuffled(pool.fallback(), random));
                Placed child = null;
                for (int c = 0; c < candidates.size() && child == null; c++) {
                    StructureTemplate candidate = candidates.get(c);
                    List<Integer> rotations = new ArrayList<>(List.of(0, 1, 2, 3));
                    Collections.shuffle(rotations, random);
                    for (int r = 0; r < rotations.size() && child == null; r++) {
                        int childRotation = rotations.get(r);
                        List<Jigsaw> childJigsaws = new ArrayList<>(MythicTemplateRewriter.jigsaws(candidate));
                        Collections.shuffle(childJigsaws, random);
                        for (Jigsaw childJigsaw : childJigsaws) {
                            String childFront = rotate(childJigsaw.front(), childRotation);
                            String childTop = rotate(childJigsaw.top(), childRotation);
                            boolean attach = childFront.equals(opposite(front))
                                && ("rollable".equals(joint(jigsaw)) || top.equals(childTop))
                                && jigsaw.target().equals(childJigsaw.name());
                            if (!attach) {
                                continue;
                            }
                            int[] local = rotatePos(childJigsaw.x(), childJigsaw.z(), childRotation);
                            Placed option = place(candidate, childRotation, targetX - local[0], targetY - childJigsaw.y(),
                                targetZ - local[1], parent.depth() + 1);
                            if (option.box().within(limit) && placed.stream().noneMatch(p -> p.box().intersects(option.box()))) {
                                child = option;
                                break;
                            }
                        }
                    }
                }
                if (child != null) {
                    placed.add(child);
                    if (child.depth() <= maxDepth) {
                        queue.addLast(child);
                    }
                }
            }
        }
        return placed;
    }

    private static int priority(Jigsaw jigsaw) {
        return jigsaw.nbt() == null || !(jigsaw.nbt().get("selection_priority") instanceof dev.linqfy.bigCasares.dimension.nbt.Nbt.IntTag tag)
            ? 0 : tag.value();
    }

    private static String joint(Jigsaw jigsaw) {
        return jigsaw.nbt() == null ? "aligned" : String.valueOf(jigsaw.nbt().getString("joint"));
    }

    private List<StructureTemplate> shuffled(String pool, Random random) {
        MythicPackIndex.Pool definition = index.pools.get(pool);
        List<StructureTemplate> result = new ArrayList<>();
        if (definition == null) {
            return result;
        }
        for (MythicPackIndex.Element element : definition.elements()) {
            for (int copy = 0; copy < element.weight(); copy++) {
                result.add(index.templates.get(element.location()));
            }
        }
        Collections.shuffle(result, random);
        return result;
    }

    private static Placed place(StructureTemplate template, int rotation, int x, int y, int z, int depth) {
        int[] size = MythicTemplateRewriter.size(template);
        int[] a = rotatePos(0, 0, rotation);
        int[] b = rotatePos(size[0] - 1, size[2] - 1, rotation);
        Box box = new Box(x + Math.min(a[0], b[0]), y, z + Math.min(a[1], b[1]),
            x + Math.max(a[0], b[0]), y + size[1] - 1, z + Math.max(a[1], b[1]));
        return new Placed(template, rotation, x, y, z, box, depth);
    }

    private static int[] worldPos(Placed piece, Jigsaw jigsaw) {
        int[] local = rotatePos(jigsaw.x(), jigsaw.z(), piece.rotation());
        return new int[]{piece.originX() + local[0], piece.originY() + jigsaw.y(), piece.originZ() + local[1]};
    }

    /** Vanilla rotation about the template origin: clockwise 90 maps (x, z) to (-z, x). */
    static int[] rotatePos(int x, int z, int rotation) {
        return switch (rotation) {
            case 1 -> new int[]{-z, x};
            case 2 -> new int[]{-x, -z};
            case 3 -> new int[]{z, -x};
            default -> new int[]{x, z};
        };
    }

    static String rotate(String direction, int rotation) {
        for (int index = 0; index < HORIZONTAL.length; index++) {
            if (HORIZONTAL[index].equals(direction)) {
                return HORIZONTAL[(index + rotation) % 4];
            }
        }
        return direction;
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

    private static int[] step(String direction) {
        return switch (direction) {
            case "north" -> new int[]{0, 0, -1};
            case "south" -> new int[]{0, 0, 1};
            case "east" -> new int[]{1, 0, 0};
            case "west" -> new int[]{-1, 0, 0};
            case "up" -> new int[]{0, 1, 0};
            default -> new int[]{0, -1, 0};
        };
    }
}
