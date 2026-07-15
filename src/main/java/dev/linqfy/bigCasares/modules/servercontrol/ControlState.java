package dev.linqfy.bigCasares.modules.servercontrol;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record ControlState(
    boolean pvpBaseline,
    Optional<TimedPvpOverride> pvpOverride,
    boolean endAccessEnabled,
    boolean elytraRocketsEnabled,
    ResistanceLevel resistanceLevel,
    Map<UUID, Boolean> staffAlerts
) {

    public ControlState {
        pvpOverride = pvpOverride == null ? Optional.empty() : pvpOverride;
        resistanceLevel = resistanceLevel == null ? ResistanceLevel.OFF : resistanceLevel;
        staffAlerts = staffAlerts == null ? Map.of() : Map.copyOf(staffAlerts);
    }

    public static ControlState defaults() {
        return new ControlState(true, Optional.empty(), true, true, ResistanceLevel.OFF, Map.of());
    }
}
