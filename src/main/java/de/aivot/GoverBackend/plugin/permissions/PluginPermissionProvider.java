package de.aivot.GoverBackend.plugin.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class PluginPermissionProvider implements PermissionProvider {
    public static final String PLUGIN_CREATE = "plugin.create";
    public static final String PLUGIN_READ = "plugin.read";
    public static final String PLUGIN_UPDATE = "plugin.update";
    public static final String PLUGIN_DELETE = "plugin.delete";

    @Override
    public String getContextLabel() {
        return "Plugins";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(PLUGIN_CREATE, "Plugin erstellen", "Erlaubt das Erstellen von Plugins."),
                PermissionEntry.of(PLUGIN_READ, "Plugin anzeigen", "Erlaubt das Anzeigen und Auflisten von Plugins."),
                PermissionEntry.of(PLUGIN_UPDATE, "Plugin bearbeiten", "Erlaubt das Bearbeiten von Plugins."),
                PermissionEntry.of(PLUGIN_DELETE, "Plugin löschen", "Erlaubt das Löschen von Plugins."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
