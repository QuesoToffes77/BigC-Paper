package dev.linqfy.bigCasares.modules.shop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopModuleWiringTest {

    @Test
    void exposesStableModuleId() {
        assertEquals("shop-system", new ShopModule(null).getId());
    }
}
