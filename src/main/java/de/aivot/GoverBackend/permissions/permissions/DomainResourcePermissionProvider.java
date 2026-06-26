package de.aivot.GoverBackend.permissions.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class DomainResourcePermissionProvider implements PermissionProvider {
    public static final String DOMAIN_RESOURCE_PERMISSION_CREATE = "domain_resource_permission.create";
    public static final String DOMAIN_RESOURCE_PERMISSION_READ = "domain_resource_permission.read";
    public static final String DOMAIN_RESOURCE_PERMISSION_UPDATE = "domain_resource_permission.update";
    public static final String DOMAIN_RESOURCE_PERMISSION_DELETE = "domain_resource_permission.delete";

    @Override
    public String getContextLabel() {
        return "Domänenressourcen-Berechtigungen";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(DOMAIN_RESOURCE_PERMISSION_CREATE, "Berechtigung erstellen", "Erlaubt das Erstellen von Domänenressourcen-Berechtigungen."),
                PermissionEntry.of(DOMAIN_RESOURCE_PERMISSION_READ, "Berechtigung anzeigen", "Erlaubt das Anzeigen und Auflisten von Domänenressourcen-Berechtigungen."),
                PermissionEntry.of(DOMAIN_RESOURCE_PERMISSION_UPDATE, "Berechtigung bearbeiten", "Erlaubt das Bearbeiten von Domänenressourcen-Berechtigungen."),
                PermissionEntry.of(DOMAIN_RESOURCE_PERMISSION_DELETE, "Berechtigung löschen", "Erlaubt das Löschen von Domänenressourcen-Berechtigungen."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.Domain;
    }
}
