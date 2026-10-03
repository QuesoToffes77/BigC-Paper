package dev.linqfy.bigCasares.modules.copperapple;

import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CopperAppleMaxStackSizePolicyTest {

    @Test
    void missingComponentIsDetectedWithoutReadingItsValue() {
        ItemMeta meta = itemMeta(false, 0);

        assertTrue(CopperAppleOxidationService.requiresMaxStackSizeUpdate(meta, 64));
    }

    @Test
    void matchingComponentDoesNotNeedAnUpdate() {
        assertFalse(CopperAppleOxidationService.requiresMaxStackSizeUpdate(itemMeta(true, 64), 64));
    }

    @Test
    void differentComponentValueNeedsAnUpdate() {
        assertTrue(CopperAppleOxidationService.requiresMaxStackSizeUpdate(itemMeta(true, 16), 64));
    }

    private static ItemMeta itemMeta(boolean hasMaxStackSize, int maxStackSize) {
        return (ItemMeta) Proxy.newProxyInstance(
            ItemMeta.class.getClassLoader(),
            new Class<?>[]{ItemMeta.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "hasMaxStackSize" -> hasMaxStackSize;
                case "getMaxStackSize" -> {
                    if (!hasMaxStackSize) {
                        throw new IllegalStateException(
                            "We don't have max_stack_size! Check hasMaxStackSize first!");
                    }
                    yield maxStackSize;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
    }
}
