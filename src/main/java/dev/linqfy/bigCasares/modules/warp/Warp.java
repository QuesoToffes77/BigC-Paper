package dev.linqfy.bigCasares.modules.warp;

import org.bukkit.Location;

public final class Warp {
    private final String id;
    private String name;
    private Location location;
    private String icon;
    private String description;

    public Warp(String id, String name, Location location, String icon, String description) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.icon = icon;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        this.name = name;
    }

    public Location location() {
        return location;
    }

    public void location(Location location) {
        this.location = location;
    }

    public String icon() {
        return icon;
    }

    public void icon(String icon) {
        this.icon = icon;
    }

    public String description() {
        return description;
    }

    public void description(String description) {
        this.description = description;
    }
}
