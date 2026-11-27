package dev.linqfy.bigCasares.modules.moderation.anticheat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Limits repeated audit-only observations, with bounded storage. Time values are monotonic. */
public final class AuditThrottle {
    private final long interval;
    private final int capacity;
    private final Map<Key, Long> last = new LinkedHashMap<>();

    public AuditThrottle(long interval, int capacity) {
        if (interval <= 0 || capacity <= 0) throw new IllegalArgumentException("Invalid audit throttle limits");
        this.interval = interval;
        this.capacity = capacity;
    }

    public boolean allow(UUID player, String rule, long now) {
        Iterator<Map.Entry<Key, Long>> entries = last.entrySet().iterator();
        while (entries.hasNext()) {
            if (now - entries.next().getValue() < interval) break;
            entries.remove();
        }
        Key key = new Key(player, rule);
        if (last.containsKey(key)) return false;
        if (last.size() >= capacity) return false;
        last.put(key, now);
        return true;
    }

    public void clear() { last.clear(); }
    int size() { return last.size(); }
    private record Key(UUID player, String rule) { }
}
