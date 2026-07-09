package de.aivot.gover.backend.secrets.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

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
}
