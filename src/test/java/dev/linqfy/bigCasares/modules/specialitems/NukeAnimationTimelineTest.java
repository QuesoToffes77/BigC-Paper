package dev.linqfy.bigCasares.modules.specialitems;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NukeAnimationTimelineTest {

    @Test
    void startsWithCenterBuildsSixRingsAndReleasesAtTickThirty() {
        NukeAnimationTimeline timeline = new NukeAnimationTimeline(6, 5L);

        assertEquals(0L, timeline.seedTick());
        assertEquals(List.of(5L, 10L, 15L, 20L, 25L, 30L), timeline.ringTicks());
        assertEquals(30L, timeline.releaseTick());
    }
}
