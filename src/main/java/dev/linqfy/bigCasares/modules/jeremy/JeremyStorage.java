package dev.linqfy.bigCasares.modules.jeremy;

import java.io.IOException;
import java.util.Optional;

interface JeremyStorage {
    Optional<JeremySnapshot> load() throws IOException;

    void save(JeremySnapshot snapshot) throws IOException;
}
