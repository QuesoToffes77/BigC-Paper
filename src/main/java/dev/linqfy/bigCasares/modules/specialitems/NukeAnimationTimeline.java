package dev.linqfy.bigCasares.modules.specialitems;

import java.util.ArrayList;
import java.util.List;

record NukeAnimationTimeline(int ringCount, long ringIntervalTicks) {

    NukeAnimationTimeline {
        if (ringCount <= 0 || ringIntervalTicks <= 0L) {
            throw new IllegalArgumentException("ring count and interval must be positive");
        }
    }

    long seedTick() {
        return 0L;
    }

    List<Long> ringTicks() {
        List<Long> ticks = new ArrayList<>(ringCount);
        for (int index = 1; index <= ringCount; index++) {
            ticks.add(index * ringIntervalTicks);
        }
        return List.copyOf(ticks);
    }

    long releaseTick() {
        return ringCount * ringIntervalTicks;
    }
}
