package dev.linqfy.bigCasares.modules.forex;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** One writer per market path. Results are applied only by the owner's main-thread poll. */
final class ForexAsyncWriter implements AutoCloseable {
    private static final ConcurrentHashMap<Path, ForexAsyncWriter> LEASES = new ConcurrentHashMap<>();
    private final Path path;
    private final ExecutorService executor;
    private final ConcurrentLinkedQueue<Result> results = new ConcurrentLinkedQueue<>();
    private volatile boolean closed;
    private boolean busy;

    ForexAsyncWriter(Path path) {
        this.path = path.toAbsolutePath().normalize();
        if (LEASES.putIfAbsent(this.path, this) != null) throw new IllegalStateException("FOREX writer is still active");
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "bigcasares-forex-writer");
            thread.setDaemon(true);
            return thread;
        });
    }

    boolean busy() { return busy; }

    void submit(Runnable write, Consumer<Throwable> completion) {
        Objects.requireNonNull(write, "write");
        Objects.requireNonNull(completion, "completion");
        if (closed || busy) throw new IllegalStateException("FOREX writer unavailable");
        busy = true;
        try {
            executor.execute(() -> {
                Throwable failure = null;
                try { write.run(); } catch (Throwable thrown) { failure = thrown; }
                finally {
                    results.add(new Result(completion, failure));
                    if (closed) LEASES.remove(path, this);
                }
            });
        } catch (RuntimeException failure) {
            busy = false;
            throw failure;
        }
    }

    void poll() {
        Result result = results.poll();
        if (result == null || closed) return;
        busy = false;
        result.completion().accept(result.failure());
    }

    @Override
    public void close() {
        closed = true;
        executor.shutdown();
        try {
            if (executor.awaitTermination(250, TimeUnit.MILLISECONDS)) LEASES.remove(path, this);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private record Result(Consumer<Throwable> completion, Throwable failure) { }
}
