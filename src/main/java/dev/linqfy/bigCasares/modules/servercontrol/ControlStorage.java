package dev.linqfy.bigCasares.modules.servercontrol;

public interface ControlStorage {

    ControlState load();

    void save(ControlState state);
}
