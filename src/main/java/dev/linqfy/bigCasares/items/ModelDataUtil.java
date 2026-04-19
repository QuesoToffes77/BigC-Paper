package dev.linqfy.bigCasares.items;

import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.List;
import java.util.OptionalInt;

public final class ModelDataUtil {

    private ModelDataUtil() {
    }

    public static OptionalInt readCustomModelData(ItemMeta meta) {
        try {
            // Try modern API: ItemMeta.getCustomModelDataComponent()
            Method getComp = ItemMeta.class.getMethod("getCustomModelDataComponent");
            Object comp = getComp.invoke(meta);
            Method getFloats = comp.getClass().getMethod("getFloats");
            @SuppressWarnings("unchecked")
            List<Float> values = (List<Float>) getFloats.invoke(comp);
            if (values == null || values.isEmpty()) return OptionalInt.empty();
            return OptionalInt.of(values.get(0).intValue());
        } catch (Throwable t) {
            // Fallback to legacy API: ItemMeta.hasCustomModelData()/getCustomModelData()
            try {
                Method has = ItemMeta.class.getMethod("hasCustomModelData");
                boolean present = (boolean) has.invoke(meta);
                if (!present) return OptionalInt.empty();
                Method get = ItemMeta.class.getMethod("getCustomModelData");
                Integer val = (Integer) get.invoke(meta);
                if (val == null) return OptionalInt.empty();
                return OptionalInt.of(val);
            } catch (Throwable ignore) {
                return OptionalInt.empty();
            }
        }
    }

    public static void writeCustomModelData(ItemMeta meta, int modelData) {
        try {
            // Modern API: use CustomModelDataComponent
            Method getComp = ItemMeta.class.getMethod("getCustomModelDataComponent");
            Object comp = getComp.invoke(meta);
            Method setFloats = comp.getClass().getMethod("setFloats", List.class);
            setFloats.invoke(comp, List.of((float) modelData));
            Method setComp = ItemMeta.class.getMethod("setCustomModelDataComponent", comp.getClass());
            setComp.invoke(meta, comp);
            return;
        } catch (Throwable t) {
            // Fallback to legacy: ItemMeta.setCustomModelData(Integer)
            try {
                Method set = ItemMeta.class.getMethod("setCustomModelData", Integer.class);
                set.invoke(meta, Integer.valueOf(modelData));
            } catch (Throwable ignore) {
                // can't set model data on this runtime - give up silently
            }
        }
    }
}
