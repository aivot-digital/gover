package de.aivot.gover.backend.codeLists.permissions;

import de.aivot.gover.backend.permissions.enums.PermissionScope;
import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

@Component
public class CodeListPermissionProvider implements PermissionProvider {
    public static final String CODE_LIST_CREATE = "code_list.create";
    public static final String CODE_LIST_READ = "code_list.read";
    public static final String CODE_LIST_UPDATE = "code_list.update";
    public static final String CODE_LIST_DELETE = "code_list.delete";
    public static final String CODE_LIST_EXPORT = "code_list.export";

    @Override
    public String getContextLabel() {
        return "Code-Listen";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(CODE_LIST_CREATE, "Code-Liste erstellen", "Erlaubt das Erstellen von Code-Listen."),
                PermissionEntry.of(CODE_LIST_READ, "Code-Liste anzeigen", "Erlaubt das Anzeigen und Auflisten von Code-Listen."),
                PermissionEntry.of(CODE_LIST_UPDATE, "Code-Liste bearbeiten", "Erlaubt das Bearbeiten von Code-Listen."),
                PermissionEntry.of(CODE_LIST_DELETE, "Code-Liste löschen", "Erlaubt das Löschen von Code-Listen."),
                PermissionEntry.of(CODE_LIST_EXPORT, "Code-Liste exportieren", "Erlaubt das Exportieren von Code-Listen.")
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
