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
        return "Identity Provider";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(IDENTITY_PROVIDER_CREATE, "Identity Provider erstellen", "Erlaubt das Erstellen von Identity Providern."),
                PermissionEntry.of(IDENTITY_PROVIDER_READ, "Identity Provider anzeigen", "Erlaubt das Anzeigen und Auflisten von Identity Providern."),
                PermissionEntry.of(IDENTITY_PROVIDER_UPDATE, "Identity Provider bearbeiten", "Erlaubt das Bearbeiten von Identity Providern."),
                PermissionEntry.of(IDENTITY_PROVIDER_DELETE, "Identity Provider löschen", "Erlaubt das Löschen von Identity Providern."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
