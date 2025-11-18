package de.aivot.GoverBackend.userRoles.models;

public interface RolePermission {
    int getLevel();

    default boolean isAtLeast(RolePermission other) {
        return this.getLevel() >= other.getLevel();
    }
}
