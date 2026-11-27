package de.aivot.prosuna.backend.secrets.permissions;

import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecretPermissionProvider implements PermissionProvider {
    public static final String SECRET_CREATE = "secret.create";
    public static final String SECRET_READ = "secret.read";
    public static final String SECRET_UPDATE = "secret.update";
    public static final String SECRET_DELETE = "secret.delete";

    @Override
    public String getContextLabel() {
        return "Geheimnisse";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(SECRET_CREATE, "Geheimnis erstellen", "Erlaubt das Erstellen von Geheimnissen."),
                PermissionEntry.of(SECRET_READ, "Geheimnis anzeigen", "Erlaubt das Anzeigen und Auflisten von Geheimnissen."),
                PermissionEntry.of(SECRET_UPDATE, "Geheimnis bearbeiten", "Erlaubt das Bearbeiten von Geheimnissen."),
                PermissionEntry.of(SECRET_DELETE, "Geheimnis löschen", "Erlaubt das Löschen von Geheimnissen."),
        };
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "secrets",
                SECRET_READ
        ));
    }
}
