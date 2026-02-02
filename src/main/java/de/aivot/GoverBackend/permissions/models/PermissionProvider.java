package de.aivot.GoverBackend.permissions.models;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;

public interface PermissionProvider {
    String getContextLabel();
    PermissionEntry[] getPermissions();
    PermissionScope getScope();
}
