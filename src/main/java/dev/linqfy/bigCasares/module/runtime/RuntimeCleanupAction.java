package dev.linqfy.bigCasares.module.runtime;

@FunctionalInterface
public interface RuntimeCleanupAction {

    void clean() throws Exception;
}
