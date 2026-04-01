package de.aivot.GoverBackend.identity.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class IdentityProviderPermissionProvider implements PermissionProvider {
    public static final String IDENTITY_PROVIDER_CREATE = "identity_provider.create";
    public static final String IDENTITY_PROVIDER_READ = "identity_provider.read";
    public static final String IDENTITY_PROVIDER_UPDATE = "identity_provider.update";
    public static final String IDENTITY_PROVIDER_DELETE = "identity_provider.delete";

    @Override
    public String getContextLabel() {
        return "Identitätsanbieter";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(IDENTITY_PROVIDER_CREATE, "Identitätsanbieter erstellen", "Erlaubt das Erstellen von Identitätsanbietern."),
                PermissionEntry.of(IDENTITY_PROVIDER_READ, "Identitätsanbieter anzeigen", "Erlaubt das Anzeigen und Auflisten von Identitätsanbietern."),
                PermissionEntry.of(IDENTITY_PROVIDER_UPDATE, "Identitätsanbieter bearbeiten", "Erlaubt das Bearbeiten von Identitätsanbietern."),
                PermissionEntry.of(IDENTITY_PROVIDER_DELETE, "IIdentitätsanbieter löschen", "Erlaubt das Löschen von Identitätsanbietern."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
