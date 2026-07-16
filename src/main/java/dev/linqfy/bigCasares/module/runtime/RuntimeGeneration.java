package dev.linqfy.bigCasares.module.runtime;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class RuntimeGeneration {

    private final long id;
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock(true);
    private boolean active = true;

    public RuntimeGeneration(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("Generation id must be non-negative");
        }
        this.id = id;
    }

    public long id() {
        return id;
    }

    public boolean isActive() {
        lifecycleLock.readLock().lock();
        try {
            return active;
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    public boolean retire() {
        lifecycleLock.writeLock().lock();
        try {
            if (!active) {
                return false;
            }
            active = false;
            return true;
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    public Runnable guard(Runnable callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback is required");
        }
        return () -> {
            lifecycleLock.readLock().lock();
            try {
                if (active) {
                    callback.run();
                }
            } finally {
                lifecycleLock.readLock().unlock();
            }
        };
    }
}
