package de.aivot.GoverBackend.userRoles.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class SystemRolePermissionProvider implements PermissionProvider {
    public static final String SYSTEM_ROLE_CREATE = "system_role.create";
    public static final String SYSTEM_ROLE_READ = "system_role.read";
    public static final String SYSTEM_ROLE_UPDATE = "system_role.update";
    public static final String SYSTEM_ROLE_DELETE = "system_role.delete";

    @Override
    public String getContextLabel() {
        return "Systemrollen";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(SYSTEM_ROLE_CREATE, "Systemrolle erstellen", "Erlaubt das Erstellen von Systemrollen."),
                PermissionEntry.of(SYSTEM_ROLE_READ, "Systemrolle anzeigen", "Erlaubt das Anzeigen und Auflisten von Systemrollen."),
                PermissionEntry.of(SYSTEM_ROLE_UPDATE, "Systemrolle bearbeiten", "Erlaubt das Bearbeiten von Systemrollen."),
                PermissionEntry.of(SYSTEM_ROLE_DELETE, "Systemrolle löschen", "Erlaubt das Löschen von Systemrollen."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
