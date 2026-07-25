package dev.linqfy.bigCasares.modules.endevent;

import java.time.Duration;

public enum EndEventMilestone {
    POTIONS_LOCKED(Duration.ofMinutes(8)),
    NETHER_WARNING(Duration.ofMinutes(9)),
    NETHER_LOCKED(Duration.ofMinutes(10)),
    END_WARNING(Duration.ofMinutes(14)),
    END_LOCKED(Duration.ofMinutes(15)),
    BORDER_FINISHED(Duration.ofMinutes(30));

    private final Duration offset;

    EndEventMilestone(Duration offset) {
        this.offset = offset;
    }

    public Duration offset() {
        return offset;
    }
}
