package dev.linqfy.bigCasares.modules.customcrossbow;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomCrossbowLoadServiceTest {

    @Test
    void timedLoadingCompletesAfterConfiguredTicksAndConsumesOneItem() {
        CustomCrossbowLoadService service = new CustomCrossbowLoadService();
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertTrue(service.start(playerId, CustomCrossbowChargeType.AMETHYST_SHARD, 0, Material.AMETHYST_SHARD, 3));
        Optional<CustomCrossbowLoadResult> early = service.tick(playerId, Material.AMETHYST_SHARD, 3, 29);
        Optional<CustomCrossbowLoadResult> completed = service.tick(playerId, Material.AMETHYST_SHARD, 3, 30);

        assertTrue(early.isEmpty());
        assertTrue(completed.isPresent());
        assertEquals(CustomCrossbowChargeType.AMETHYST_SHARD, completed.get().chargeType());
        assertEquals(2, completed.get().remainingOffhandAmount());
    }

    @Test
    void loadingCancelsWhenOffhandItemChanges() {
        CustomCrossbowLoadService service = new CustomCrossbowLoadService();
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        service.start(playerId, CustomCrossbowChargeType.ECHO_SHARD, 0, Material.ECHO_SHARD, 1);
        Optional<CustomCrossbowLoadResult> result = service.tick(playerId, Material.ENDER_PEARL, 1, 45);

        assertTrue(result.isEmpty());
        assertFalse(service.isLoading(playerId));
    }

    @Test
    void duplicateStartDoesNotRestartActiveLoad() {
        CustomCrossbowLoadService service = new CustomCrossbowLoadService();
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        assertTrue(service.start(playerId, CustomCrossbowChargeType.ECHO_SHARD, 0, Material.ECHO_SHARD, 1));
        assertFalse(service.start(playerId, CustomCrossbowChargeType.ENDER_PEARL, 0, Material.ENDER_PEARL, 1));
    }
}
