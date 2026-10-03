package dev.linqfy.bigCasares.modules.grapplinghook;

import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrapplingHookModuleWiringTest {

    @Test
    void moduleIdMatchesConfigGate() {
        assertEquals("grappling-hook", new GrapplingHookModule(null).getId());
    }

    @Test
    void lifecycleIsSafeWithoutPlugin() {
        GrapplingHookModule module = new GrapplingHookModule(null);
        module.onEnable();
        module.onDisable();
    }

    @Test
    void lifecycleIsSafeWithExplicitScope() {
        GrapplingHookModule module = new GrapplingHookModule(null);
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        module.onEnable(scope);
        module.onDisable();
        scope.close();
    }
}
