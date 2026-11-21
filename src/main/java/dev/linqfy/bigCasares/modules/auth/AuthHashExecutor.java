package dev.linqfy.bigCasares.modules.auth;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** A fixed-size queue for CPU-intensive password and pattern hashing. */
final class AuthHashExecutor implements AutoCloseable {
    private final ThreadPoolExecutor executor;

    AuthHashExecutor(int workers, int queued) {
        executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queued), task -> {
                Thread thread = new Thread(task, "BigCasares-Auth-Hash");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    }

    boolean submit(Runnable work, Runnable rejected) {
        try {
            executor.execute(work);
            return true;
        } catch (RejectedExecutionException unavailable) {
            rejected.run();
            return false;
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
