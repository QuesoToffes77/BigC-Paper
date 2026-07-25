package dev.linqfy.bigCasares.modules.endevent;

import java.util.Optional;

public interface EndEventStorage {

    Optional<EndEventSnapshot> load();

    void save(EndEventSnapshot snapshot);
}
