package dev.linqfy.bigCasares.modules.resourcepack;

import java.util.Optional;
import java.util.UUID;

public interface PackReloadOperation {

    Optional<ActivePackManifest> activeManifest() throws Exception;

    PackPublication prepare(UUID jobId) throws Exception;

    void commit(PackPublication publication) throws Exception;
}
