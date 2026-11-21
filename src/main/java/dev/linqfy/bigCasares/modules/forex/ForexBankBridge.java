package dev.linqfy.bigCasares.modules.forex;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Bridges worker requests to Vault on the server thread; shutdown rejects queued requests. */
final class ForexBankBridge implements ForexBank, AutoCloseable {
    private final ForexBank delegate;
    private final BooleanSupplier onMainThread;
    private final ArrayDeque<Request<?>> pending = new ArrayDeque<>();
    private boolean closed;

    ForexBankBridge(ForexBank delegate, BooleanSupplier onMainThread) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.onMainThread = Objects.requireNonNull(onMainThread, "onMainThread");
    }

    @Override public long balance(UUID player) { return call(() -> delegate.balance(player)); }
    @Override public boolean withdraw(UUID player, long amount) { return call(() -> delegate.withdraw(player, amount)); }
    @Override public boolean deposit(UUID player, long amount) { return call(() -> delegate.deposit(player, amount)); }

    private <T> T call(Supplier<T> operation) {
        Request<T> request;
        synchronized (pending) {
            if (closed) throw new IllegalStateException("FOREX Vault bridge is closed");
            if (onMainThread.getAsBoolean()) return operation.get();
            request = new Request<>(operation, new CompletableFuture<>());
            pending.addLast(request);
        }
        try {
            return request.result().get();
        } catch (InterruptedException interrupted) {
            synchronized (pending) { pending.remove(request); }
            Thread.currentThread().interrupt();
            throw new IllegalStateException("FOREX Vault request interrupted", interrupted);
        } catch (ExecutionException failure) {
            throw new IllegalStateException("FOREX Vault request failed", failure.getCause());
        }
    }

    /** Called only by the scope-owned task on the server thread. */
    void pump() {
        if (!onMainThread.getAsBoolean()) throw new IllegalStateException("Vault must run on the server thread");
        Request<?> request;
        synchronized (pending) { request = pending.pollFirst(); }
        if (request != null) request.execute();
    }

    int pendingCount() {
        synchronized (pending) { return pending.size(); }
    }

    @Override public void close() {
        if (!onMainThread.getAsBoolean()) throw new IllegalStateException("Close must run on the server thread");
        synchronized (pending) {
            closed = true;
            Request<?> request;
            while ((request = pending.pollFirst()) != null) {
                request.result().completeExceptionally(new IllegalStateException("FOREX Vault bridge closed"));
            }
        }
    }

    private record Request<T>(Supplier<T> action, CompletableFuture<T> result) {
        void execute() {
            try { result.complete(action.get()); }
            catch (Throwable failure) { result.completeExceptionally(failure); }
        }
    }
}
