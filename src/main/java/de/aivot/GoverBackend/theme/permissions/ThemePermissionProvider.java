package de.aivot.GoverBackend.theme.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class ThemePermissionProvider implements PermissionProvider {
    public static final String THEME_CREATE = "theme.create";
    public static final String THEME_READ = "theme.read";
    public static final String THEME_UPDATE = "theme.update";
    public static final String THEME_DELETE = "theme.delete";

    @Override
    public String getContextLabel() {
        return "Design";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(THEME_CREATE, "Theme erstellen", "Erlaubt das Erstellen von Themes."),
                PermissionEntry.of(THEME_READ, "Theme anzeigen", "Erlaubt das Anzeigen und Auflisten von Themes."),
                PermissionEntry.of(THEME_UPDATE, "Theme bearbeiten", "Erlaubt das Bearbeiten von Themes."),
                PermissionEntry.of(THEME_DELETE, "Theme löschen", "Erlaubt das Löschen von Themes."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
