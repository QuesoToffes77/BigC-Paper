package dev.linqfy.bigCasares.modules.donpollos;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DonPollosModuleWiringTest {

    @Test
    void moduleIdIsStable() {
        assertEquals("don-pollos", new DonPollosModule(null).getId());
        assertEquals("don-pollos", DonPollosModule.MODULE_ID);
    }

    @Test
    void enablingWithoutAPluginIsANoOp() {
        DonPollosModule module = new DonPollosModule(null);
        module.onEnable();
        module.onDisable();
    }

    @Test
    void menusAreClosedBeforeTheirListenersAreRemoved() throws IOException {
        // Las limpiezas de BigC corren en orden inverso al registro (RuntimeRegistrationScopeTest). Para que los
        // menus se cierren mientras sus listeners todavia protegen los clicks y devuelven apuestas e ingredientes,
        // "don-pollos-close-menus" tiene que registrarse despues de todos los listeners.
        String source = Files.readString(Path.of(
            "src/main/java/dev/linqfy/bigCasares/modules/donpollos/DonPollosModule.java"));
        int closeMenus = source.indexOf("ownCleanup(\"don-pollos-close-menus\"");
        int lastListener = source.lastIndexOf("registerListener(");
        assertTrue(closeMenus > 0, "falta la limpieza que cierra los menus");
        assertTrue(closeMenus > lastListener, "la limpieza de menus tiene que registrarse despues de los listeners");
    }
}
