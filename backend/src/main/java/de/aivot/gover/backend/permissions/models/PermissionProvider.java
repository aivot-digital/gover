package de.aivot.gover.backend.permissions.models;

import de.aivot.gover.backend.permissions.enums.PermissionScope;

public interface PermissionProvider {
    String getContextLabel();
    PermissionEntry[] getPermissions();
    PermissionScope getScope();
}
