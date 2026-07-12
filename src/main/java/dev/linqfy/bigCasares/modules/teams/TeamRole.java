package dev.linqfy.bigCasares.modules.teams;

public enum TeamRole {
    OWNER,
    ADMIN,
    MEMBER;

    public boolean canEditAppearance() {
        return this == OWNER;
    }

    public boolean canViewAppearance() {
        return this == OWNER || this == ADMIN;
    }
}
