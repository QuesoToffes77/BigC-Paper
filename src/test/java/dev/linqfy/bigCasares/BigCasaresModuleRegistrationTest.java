package dev.linqfy.bigCasares;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BigCasaresModuleRegistrationTest {

    @Test
    void registersFrameworkV2ModulesExactlyOnceInDependencyOrder() {
        assertEquals(List.of(
            "resource-pack-system",
            "team-system",
            "nexus-system",
            "entity-shop-system",
            "pve-boss-system",
            "geyser-integration"
        ), BigCasares.frameworkV2ModuleOrder());
    }
}
