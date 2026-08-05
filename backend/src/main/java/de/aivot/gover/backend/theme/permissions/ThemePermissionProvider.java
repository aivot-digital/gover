package de.aivot.gover.backend.theme.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ThemePermissionProvider implements PermissionProvider {
    public static final String THEME_CREATE = "theme.create";
    public static final String THEME_READ = "theme.read";
    public static final String THEME_UPDATE = "theme.update";
    public static final String THEME_DELETE = "theme.delete";

    @Override
    public String getContextLabel() {
        return "Erscheinungsbild";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(THEME_CREATE, "Erscheinungsbild erstellen", "Erlaubt das Erstellen von Erscheinungsbildern."),
                PermissionEntry.of(THEME_READ, "Erscheinungsbild anzeigen", "Erlaubt das Anzeigen und Auflisten von Erscheinungsbildern."),
                PermissionEntry.of(THEME_UPDATE, "Erscheinungsbild bearbeiten", "Erlaubt das Bearbeiten von Erscheinungsbildern."),
                PermissionEntry.of(THEME_DELETE, "Erscheinungsbild löschen", "Erlaubt das Löschen von Erscheinungsbildern."),
        };
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "themes",
                THEME_READ
        ));
    }
}
