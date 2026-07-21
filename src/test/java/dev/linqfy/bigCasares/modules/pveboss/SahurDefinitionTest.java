package dev.linqfy.bigCasares.modules.pveboss;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SahurDefinitionTest {

    @Test
    void loadsRaidScalingAnimationsAndEveryRequestedMove() {
        var stream = getClass().getClassLoader().getResourceAsStream("bosses/tung-tung-sahur.yml");
        assertNotNull(stream, "missing Sahur boss definition");
        AbyssGuardianDefinition definition = new AbyssGuardianDefinitionLoader().load(
            YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));

        assertEquals("tung-tung-sahur", definition.id());
        assertEquals("boss_sahur", definition.javaModelKey());
        assertEquals(6_767.0, definition.maximumHealth());
        assertEquals(6_767.0, definition.maximumHealthFor(10));
        assertEquals(6_767.0, definition.maximumHealthFor(20));
        assertEquals(
            Set.of("idle", "walk", "sprint", "bat_hit", "spin_in_place", "stomp"),
            definition.animations().keySet()
        );

        assertEquals(
            List.of("bat-hit", "stomp", "wall-charge", "spin-attack", "hunter-bat", "shield-bats", "launch-bat"),
            definition.abilities().stream().map(BossAbilityDefinition::id).toList()
        );
        Set<BossAbilityEffectType> effects = definition.abilities().stream()
            .flatMap(ability -> ability.effects().stream())
            .map(BossAbilityEffectDefinition::type)
            .collect(Collectors.toSet());
        assertTrue(effects.containsAll(Set.of(
            BossAbilityEffectType.SAHUR_BAT_HIT,
            BossAbilityEffectType.SAHUR_STOMP,
            BossAbilityEffectType.SAHUR_CHARGE,
            BossAbilityEffectType.SAHUR_SPIN,
            BossAbilityEffectType.SAHUR_HUNTER_BAT,
            BossAbilityEffectType.SAHUR_SHIELD_BATS,
            BossAbilityEffectType.SAHUR_LAUNCH_BAT
        )));
    }
}
