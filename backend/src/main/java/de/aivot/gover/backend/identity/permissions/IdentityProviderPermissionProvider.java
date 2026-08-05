package de.aivot.gover.backend.identity.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
                PermissionEntry.of(IDENTITY_PROVIDER_DELETE, "Identitätsanbieter löschen", "Erlaubt das Löschen von Identitätsanbietern."),
        };
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "identity_providers",
                IDENTITY_PROVIDER_READ
        ));
    }
}
