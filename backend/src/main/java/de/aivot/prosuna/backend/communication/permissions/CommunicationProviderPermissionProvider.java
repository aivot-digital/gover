package de.aivot.prosuna.backend.communication.permissions;

import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CommunicationProviderPermissionProvider implements PermissionProvider {
    public static final String COMMUNICATION_PROVIDER_CREATE = "communication_provider.create";
    public static final String COMMUNICATION_PROVIDER_READ = "communication_provider.read";
    public static final String COMMUNICATION_PROVIDER_UPDATE = "communication_provider.update";
    public static final String COMMUNICATION_PROVIDER_DELETE = "communication_provider.delete";

    @Override
    public String getContextLabel() {
        return "Kommunikationsanbieter";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(COMMUNICATION_PROVIDER_CREATE, "Kommunikationsanbieter erstellen", "Erlaubt das Erstellen von Kommunikationsanbietern und Anbindungen."),
                PermissionEntry.of(COMMUNICATION_PROVIDER_READ, "Kommunikationsanbieter anzeigen", "Erlaubt das Anzeigen von Kommunikationsanbietern und Anbindungen."),
                PermissionEntry.of(COMMUNICATION_PROVIDER_UPDATE, "Kommunikationsanbieter bearbeiten", "Erlaubt das Bearbeiten von Kommunikationsanbietern und Anbindungen."),
                PermissionEntry.of(COMMUNICATION_PROVIDER_DELETE, "Kommunikationsanbieter löschen", "Erlaubt das Löschen von Kommunikationsanbietern und Anbindungen."),
        };
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new SearchPermission("communication_providers", COMMUNICATION_PROVIDER_READ));
    }
}
