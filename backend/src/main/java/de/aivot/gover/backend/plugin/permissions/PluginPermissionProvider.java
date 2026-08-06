package de.aivot.gover.backend.plugin.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class PluginPermissionProvider implements PermissionProvider {
    public static final String PLUGIN_READ = "plugin.read";

    @Override
    public String getContextLabel() {
        return "Erweiterungen";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(
                        PLUGIN_READ,
                        "Erweiterungen anzeigen",
                        "Erlaubt nur das Anzeigen der Seite „Erweiterungen“ und das Einsehen installierter Plugins sowie deren Metadaten."
                ),
        };
    }
}
