package dev.linqfy.bigCasares.modules.airdrop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirdropAutoSchedulerTest {

    private static final long INTERVAL_MILLIS = 60 * 60_000L; // 60 minutes

    private FakeClock clock;
    private FakeScheduler scheduler;
    private FakeDeadlineStore deadlines;
    private AtomicInteger triggers;
    private AirdropAutoScheduler auto;

    @BeforeEach
    void setUp() {
        clock = new FakeClock(1_000_000L);
        scheduler = new FakeScheduler();
        deadlines = new FakeDeadlineStore();
        triggers = new AtomicInteger();
        auto = new AirdropAutoScheduler(
            clock,
            scheduler,
            deadlines,
            INTERVAL_MILLIS,
            triggers::incrementAndGet,
            ignored -> { }
        );
    }

    @Test
    void startWithNoPersistedDeadlineSchedulesOneFullInterval() {
        auto.start();

        assertEquals(1, scheduler.scheduledCount());
        assertEquals(1, scheduler.activeCount());
        assertEquals(72_000L, scheduler.lastDelayTicks());
        assertEquals(clock.now + INTERVAL_MILLIS, deadlines.persisted);
        assertTrue(auto.isRunning());
    }

    @Test
    void startResumesRemainingTimeWhenDeadlineIsInTheFuture() {
        deadlines.persisted = clock.now + 30 * 60_000L;

        auto.start();

        assertEquals(36_000L, scheduler.lastDelayTicks());
        assertEquals(deadlines.persisted, deadlines.persistedAfterNextSchedule());
    }

    @Test
    void startFiresMissedDropImmediatelyWhenDeadlineAlreadyPassed() {
        deadlines.persisted = clock.now - 60_000L;

        auto.start();

        assertEquals(1L, scheduler.lastDelayTicks());
    }

    @Test
    void firingRunsTriggerAndSchedulesNextFullInterval() {
        auto.start();
        scheduler.fireLast();

        assertEquals(1, triggers.get());
        assertEquals(clock.now + INTERVAL_MILLIS, deadlines.persistedAfterNextSchedule());
        // Initial task is done; one fresh full-interval task is active.
        assertEquals(2, scheduler.scheduledCount());
        assertEquals(1, scheduler.activeCount());
        assertEquals(72_000L, scheduler.lastDelayTicks());
    }

    @Test
    void firingMissedDropReschedulesFreshIntervalInsteadOfImmediateLoop() {
        deadlines.persisted = clock.now - 60_000L;
        auto.start();
        scheduler.fireLast();

        assertEquals(1, triggers.get());
        assertEquals(1, scheduler.activeCount());
        assertEquals(72_000L, scheduler.lastDelayTicks());
    }

    @Test
    void stopCancelsPendingTask() {
        auto.start();
        auto.stop();

        assertFalse(auto.isRunning());
        assertEquals(0, scheduler.activeCount());
    }

    @Test
    void startTwiceIsIdempotent() {
        auto.start();
        auto.start();

        assertEquals(1, scheduler.scheduledCount());
        assertEquals(1, scheduler.activeCount());
    }

    @Test
    void restartAfterStopNeverDuplicatesSchedulers() {
        auto.start();
        auto.stop();
        auto.start();

        assertEquals(2, scheduler.scheduledCount());
        assertEquals(1, scheduler.activeCount());
        assertTrue(scheduler.firstTask().cancelled);
    }

    @Test
    void triggerAfterStopIsIgnored() {
        auto.start();
        auto.stop();
        scheduler.fireLast();

        assertEquals(0, triggers.get());
        // The initial task was cancelled by stop and never rescheduled itself.
        assertEquals(1, scheduler.scheduledCount());
        assertEquals(0, scheduler.activeCount());
    }

    private static final class FakeClock implements AirdropAutoScheduler.Clock {
        private long now;

        private FakeClock(long now) {
            this.now = now;
        }

        @Override
        public long millis() {
            return now;
        }
    }

    private static final class FakeDeadlineStore implements AirdropAutoScheduler.DeadlineStore {
        private long persisted = -1;

        @Override
        public OptionalLong nextDropAtMillis() {
            return persisted < 0 ? OptionalLong.empty() : OptionalLong.of(persisted);
        }

        @Override
        public void nextDropAtMillis(long epochMillis) {
            persisted = epochMillis;
        }

        private long persistedAfterNextSchedule() {
            return persisted;
        }
    }

    private static final class FakeScheduler implements AirdropAutoScheduler.Scheduler {
        private final List<FakeTask> tasks = new ArrayList<>();
        private long lastDelayTicks = -1;

        @Override
        public AirdropAutoScheduler.ScheduledTask schedule(long delayTicks, Runnable action) {
            lastDelayTicks = delayTicks;
            FakeTask task = new FakeTask(action);
            tasks.add(task);
            return task;
        }

        private long lastDelayTicks() {
            return lastDelayTicks;
        }

        private int scheduledCount() {
            return tasks.size();
        }

        private int activeCount() {
            return (int) tasks.stream().filter(task -> !task.cancelled).count();
        }

        private FakeTask firstTask() {
            return tasks.get(0);
        }

        private void fireLast() {
            tasks.get(tasks.size() - 1).action.run();
        }
    }

    private static final class FakeTask implements AirdropAutoScheduler.ScheduledTask {
        private final Runnable action;
        private boolean cancelled;

        private FakeTask(Runnable action) {
            this.action = action;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
