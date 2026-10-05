package de.aivot.prosuna.backend.user.permissions;

import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPermissionProvider implements PermissionProvider {
    public static final String USER_CREATE = "user.create";
    public static final String USER_READ = "user.read";
    public static final String USER_UPDATE = "user.update";
    public static final String USER_DELETE = "user.delete";

    public static final String DEPUTY_CREATE = "deputy.create";
    public static final String DEPUTY_READ = "deputy.read";
    public static final String DEPUTY_UPDATE = "deputy.update";
    public static final String DEPUTY_DELETE = "deputy.delete";

    @Override
    public String getContextLabel() {
        return "Benutzer";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(USER_CREATE, "Benutzer erstellen", "Erlaubt das Erstellen von Benutzern."),
                PermissionEntry.of(USER_READ, "Benutzer anzeigen", "Erlaubt das Anzeigen und Auflisten von Benutzern."),
                PermissionEntry.of(USER_UPDATE, "Benutzer bearbeiten", "Erlaubt das Bearbeiten von Benutzern."),
                PermissionEntry.of(USER_DELETE, "Benutzer löschen", "Erlaubt das Löschen von Benutzern."),
                PermissionEntry.of(DEPUTY_CREATE, "Vertretung erstellen", "Erlaubt das Erstellen von Vertretungen."),
                PermissionEntry.of(DEPUTY_READ, "Vertretung anzeigen", "Erlaubt das Anzeigen und Auflisten von Vertretungen."),
                PermissionEntry.of(DEPUTY_UPDATE, "Vertretung bearbeiten", "Erlaubt das Bearbeiten von Vertretungen."),
                PermissionEntry.of(DEPUTY_DELETE, "Vertretung löschen", "Erlaubt das Löschen von Vertretungen."),
        };
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "users",
                USER_READ
        ));
    }
}
