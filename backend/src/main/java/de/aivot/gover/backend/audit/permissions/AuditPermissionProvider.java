package de.aivot.gover.backend.audit.permissions;

import de.aivot.gover.backend.permissions.enums.PermissionScope;
import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class AuditPermissionProvider implements PermissionProvider {
    public static final String AUDIT_LOG_READ = "audit_log.read";

    @Override
    public String getContextLabel() {
        return "Audit-Logs";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(
                        AUDIT_LOG_READ,
                        "Audit-Logs anzeigen",
                        "Erlaubt das Anzeigen und Auflisten von Audit-Logs."
                )
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
