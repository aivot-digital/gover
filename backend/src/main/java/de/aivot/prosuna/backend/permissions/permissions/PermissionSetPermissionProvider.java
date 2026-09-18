package de.aivot.prosuna.backend.permissions.permissions;

import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class PermissionSetPermissionProvider implements PermissionProvider {
    public static final String PERMISSION_SET_READ = "permission_set.read";

    @Override
    public String getContextLabel() {
        return "Berechtigungssets";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(PERMISSION_SET_READ, "Berechtigungsset anzeigen", "Erlaubt das Anzeigen effektiver Berechtigungen anderer Benutzer:innen."),
        };
    }
}
