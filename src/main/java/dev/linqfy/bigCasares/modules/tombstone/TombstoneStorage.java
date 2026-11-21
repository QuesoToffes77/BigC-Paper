package dev.linqfy.bigCasares.modules.tombstone;

import java.util.Collection;
import java.util.List;

public interface TombstoneStorage {
    List<TombstoneRecord> loadAll();
    void saveAll(Collection<TombstoneRecord> records);

    default void saveBurials(Collection<TombstoneRecord> records) {
        saveAll(records);
    }
}
