package de.aivot.GoverBackend.userRoles.enums;

import de.aivot.GoverBackend.userRoles.models.RolePermission;

public enum DepartmentPermission implements RolePermission {
    Read(0),
    Write(1);

    private final int level;

    DepartmentPermission(int level) {
        this.level = level;
    }

    @Override
    public int getLevel() {
        return level;
    }
}
