package dev.linqfy.bigCasares.modules.pveboss;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BossModelAnimationMapperTest {

    @Test
    void mapsTheSupportedBossAnimationNames() {
        var mapper = new BossModelAnimationMapper();

        assertEquals("idle", mapper.forDefinition("idle"));
        assertEquals("cast", mapper.forDefinition("cast"));
        assertEquals("rage", mapper.forDefinition("rage"));
        assertEquals("death", mapper.forDefinition("death"));
    }
}
