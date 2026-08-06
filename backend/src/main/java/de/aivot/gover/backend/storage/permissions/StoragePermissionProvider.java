package de.aivot.gover.backend.storage.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StoragePermissionProvider implements PermissionProvider {
    public static final String STORAGE_PROVIDER_CREATE = "storage_provider.create";
    public static final String STORAGE_PROVIDER_READ = "storage_provider.read";
    public static final String STORAGE_PROVIDER_UPDATE = "storage_provider.update";
    public static final String STORAGE_PROVIDER_DELETE = "storage_provider.delete";

    @Override
    public String getContextLabel() {
        return "Speicheranbieter";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(
                        STORAGE_PROVIDER_CREATE,
                        "Speicheranbieter erstellen",
                        "Erlaubt das Erstellen neuer Speicheranbieter."
                ),
                PermissionEntry.of(
                        STORAGE_PROVIDER_READ,
                        "Speicheranbieter anzeigen",
                        "Erlaubt das Anzeigen und Auflisten von Speicheranbietern."
                ),
                PermissionEntry.of(
                        STORAGE_PROVIDER_UPDATE,
                        "Speicheranbieter bearbeiten",
                        "Erlaubt das Bearbeiten bestehender Speicheranbieter."
                ),
                PermissionEntry.of(
                        STORAGE_PROVIDER_DELETE,
                        "Speicheranbieter löschen",
                        "Erlaubt das Löschen von Speicheranbietern."
                )
        };
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "storage_providers",
                STORAGE_PROVIDER_READ
        ));
    }
}
