package dev.linqfy.bigCasares.modules.tombstone;

import java.util.Collection;
import java.util.List;

public interface TombstoneStorage {
    List<TombstoneRecord> loadAll();
    void saveAll(Collection<TombstoneRecord> records);
}
