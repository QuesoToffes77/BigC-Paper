package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.thirdparty;

import dev.linqfy.bigCasares.dimension.datapack.GeneratedDatapack;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.CompoundTag;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.IntTag;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.ListTag;
import dev.linqfy.bigCasares.dimension.nbt.Nbt.Tag;
import dev.linqfy.bigCasares.dimension.worldgen.StructureTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reaches the worldgen package's test helpers ({@code MythicPackIndex}, {@code MythicJigsawSimulator})
 * from this package. They are package-private in {@code worldgen}; reflection keeps this test inside
 * the third-party package without duplicating the jigsaw simulation.
 */
final class PackHarness {

    private static final String WORLDGEN = "dev.linqfy.bigCasares.modules.mythicdungeons.worldgen.";

    record Jigsaw(int x, int y, int z, String front, String name, String target, String pool) {
    }

    record Placed(String path, int[] box) {
    }

    private final Object index;
    private final Object simulator;

    PackHarness(GeneratedDatapack pack, int maxDepth, int maxDistance) {
        try {
            Class<?> indexType = Class.forName(WORLDGEN + "MythicPackIndex");
            Constructor<?> indexConstructor = indexType.getDeclaredConstructor(GeneratedDatapack.class);
            indexConstructor.setAccessible(true);
            index = indexConstructor.newInstance(pack);
            Class<?> simulatorType = Class.forName(WORLDGEN + "MythicJigsawSimulator");
            Constructor<?> simulatorConstructor = simulatorType.getDeclaredConstructor(indexType, int.class, int.class);
            simulatorConstructor.setAccessible(true);
            simulator = simulatorConstructor.newInstance(index, maxDepth, maxDistance);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    Map<String, StructureTemplate> templates() {
        return (Map<String, StructureTemplate>) field(index, "templates");
    }

    /** Pool id -> [element template ids..., fallback] (fallback last). */
    @SuppressWarnings("unchecked")
    Map<String, Object> pools() {
        return (Map<String, Object>) field(index, "pools");
    }

    List<String> poolElements(String pool) {
        Object definition = pools().get(pool);
        if (definition == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object element : (List<?>) call(definition, "elements")) {
            result.add((String) call(element, "location"));
        }
        return result;
    }

    String poolFallback(String pool) {
        return (String) call(pools().get(pool), "fallback");
    }

    List<Placed> assemble(String startPool, long seed, int surfaceY, int worldMinY) {
        try {
            Method assemble = simulator.getClass().getDeclaredMethod("assemble", String.class, long.class, int.class, int.class);
            assemble.setAccessible(true);
            List<Placed> result = new ArrayList<>();
            for (Object placed : (List<?>) assemble.invoke(simulator, startPool, seed, surfaceY, worldMinY)) {
                StructureTemplate template = (StructureTemplate) call(placed, "template");
                Object box = call(placed, "box");
                int[] bounds = new int[6];
                String[] names = {"minX", "minY", "minZ", "maxX", "maxY", "maxZ"};
                for (int i = 0; i < 6; i++) {
                    bounds[i] = (int) call(box, names[i]);
                }
                result.add(new Placed(template.id().path(), bounds));
            }
            return result;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    static boolean intersects(int[] a, int[] b) {
        return a[0] <= b[3] && a[3] >= b[0] && a[1] <= b[4] && a[4] >= b[1] && a[2] <= b[5] && a[5] >= b[2];
    }

    static List<Jigsaw> jigsaws(StructureTemplate template) {
        CompoundTag root = template.root();
        ListTag palette = root.getList("palette") != null ? root.getList("palette")
            : (ListTag) root.getList("palettes").values().getFirst();
        List<Jigsaw> result = new ArrayList<>();
        for (Tag tag : root.getList("blocks").values()) {
            CompoundTag block = (CompoundTag) tag;
            CompoundTag state = (CompoundTag) palette.values().get(((IntTag) block.get("state")).value());
            if (!"minecraft:jigsaw".equals(state.getString("Name"))) {
                continue;
            }
            String orientation = state.getCompound("Properties").getString("orientation");
            List<Tag> pos = block.getList("pos").values();
            CompoundTag nbt = block.getCompound("nbt");
            result.add(new Jigsaw(((IntTag) pos.get(0)).value(), ((IntTag) pos.get(1)).value(), ((IntTag) pos.get(2)).value(),
                orientation.substring(0, orientation.indexOf('_')), nbt.getString("name"), nbt.getString("target"),
                nbt.getString("pool")));
        }
        return result;
    }

    static int[] size(StructureTemplate template) {
        List<Tag> values = template.root().getList("size").values();
        return new int[]{((IntTag) values.get(0)).value(), ((IntTag) values.get(1)).value(), ((IntTag) values.get(2)).value()};
    }

    private static Object field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Object call(Object target, String name) {
        try {
            Method method = target.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
