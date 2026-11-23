package dev.linqfy.bigCasares.modules.villagermood;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/** Loaded entities in insertion order, allowing bounded scans without copying the index. */
final class LoadedVillagerIndex<T> {
    record Entry<T>(long sequence, T value) { }

    private final Map<UUID, Long> sequenceById = new HashMap<>();
    private final NavigableMap<Long, T> entries = new TreeMap<>();
    private long sequence;

    void add(UUID id, T value) {
        Long existing = sequenceById.get(id);
        if (existing != null) {
            entries.put(existing, value);
            return;
        }
        long next = ++sequence;
        sequenceById.put(id, next);
        entries.put(next, value);
    }

    void remove(UUID id) {
        Long key = sequenceById.remove(id);
        if (key != null) entries.remove(key);
    }

    boolean remove(UUID id, T expected) {
        Long key = sequenceById.get(id);
        if (key == null || entries.get(key) != expected) return false;
        sequenceById.remove(id);
        entries.remove(key);
        return true;
    }

    long watermark() { return sequence; }
    int size() { return entries.size(); }

    Optional<Entry<T>> next(long after, long limit) {
        var next = entries.higherEntry(after);
        return next == null || next.getKey() > limit
            ? Optional.empty() : Optional.of(new Entry<>(next.getKey(), next.getValue()));
    }

    void clear() {
        sequenceById.clear();
        entries.clear();
    }
}
