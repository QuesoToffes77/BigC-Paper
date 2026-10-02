package dev.linqfy.bigCasares.modules.airdrop;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.LongConsumer;

/**
 * Owns the automatic AirDrop cadence as a single self-rescheduling one-shot
 * task: it schedules itself for the exact tick of the next drop, runs the
 * spawn trigger, persists {@code now + interval} as the next deadline and
 * schedules again. Because the deadline is persisted, a server restart or
 * plugin reload resumes the countdown instead of resetting it to a full
 * interval; a deadline that passed while the server was offline fires the
 * missed drop immediately on startup.
 *
 * <p>At most one task exists at any moment: scheduling cancels the previous
 * one and {@link #stop()} cancels the pending task, so re-initialization
 * never duplicates schedulers. All ports are injected so the cadence logic is
 * unit-testable without a Bukkit server.
 */
final class AirdropAutoScheduler {

    /** Milliseconds in one server tick (20 TPS). */
    private static final long TICK_MILLIS = 50L;

    interface Clock {
        long millis();
    }

    interface Scheduler {
        ScheduledTask schedule(long delayTicks, Runnable action);
    }

    interface ScheduledTask {
        void cancel();
    }

    interface DeadlineStore {
        OptionalLong nextDropAtMillis();

        void nextDropAtMillis(long epochMillis);
    }

    private final Clock clock;
    private final Scheduler scheduler;
    private final DeadlineStore deadlines;
    private final long intervalMillis;
    private final Runnable onTrigger;
    private final LongConsumer onScheduled;
    private ScheduledTask activeTask;
    private boolean running;

    AirdropAutoScheduler(
        Clock clock,
        Scheduler scheduler,
        DeadlineStore deadlines,
        long intervalMillis,
        Runnable onTrigger,
        LongConsumer onScheduled
    ) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.deadlines = Objects.requireNonNull(deadlines, "deadlines");
        if (intervalMillis <= 0) {
            throw new IllegalArgumentException("intervalMillis must be positive");
        }
        this.intervalMillis = intervalMillis;
        this.onTrigger = Objects.requireNonNull(onTrigger, "onTrigger");
        this.onScheduled = Objects.requireNonNull(onScheduled, "onScheduled");
    }

    /**
     * Starts the automatic cadence. When a deadline was persisted by a
     * previous run it resumes the remaining countdown; an already-passed
     * deadline fires the missed drop immediately. No-op while already running.
     */
    void start() {
        if (running) {
            return;
        }
        running = true;
        long now = clock.millis();
        long persisted = deadlines.nextDropAtMillis().orElse(now + intervalMillis);
        scheduleNext(Math.max(persisted, now));
    }

    /** Stops the cadence and cancels the pending one-shot. */
    void stop() {
        running = false;
        cancelActive();
    }

    boolean isRunning() {
        return running;
    }

    private void scheduleNext(long deadlineMillis) {
        cancelActive();
        deadlines.nextDropAtMillis(deadlineMillis);
        long delayTicks = Math.max(1L, (deadlineMillis - clock.millis() + TICK_MILLIS - 1) / TICK_MILLIS);
        onScheduled.accept(delayTicks);
        activeTask = scheduler.schedule(delayTicks, () -> {
            if (!running) {
                return;
            }
            onTrigger.run();
            if (running) {
                // Next cadence starts a fresh full interval from the fire moment.
                scheduleNext(clock.millis() + intervalMillis);
            }
        });
    }

    private void cancelActive() {
        if (activeTask != null) {
            activeTask.cancel();
            activeTask = null;
        }
    }
}
