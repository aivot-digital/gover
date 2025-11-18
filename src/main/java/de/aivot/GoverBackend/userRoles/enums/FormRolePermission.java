package de.aivot.GoverBackend.userRoles.enums;

import de.aivot.GoverBackend.userRoles.models.RolePermission;

public enum FormRolePermission implements RolePermission {
    None(0),
    Read(1),
    Review(2),
    Write(3),
    Publish(4),
    Admin(99);

    private final int level;

    FormRolePermission(int level) {
        this.level = level;
    }

    @Override
    public int getLevel() {
        return level;
    }
}
