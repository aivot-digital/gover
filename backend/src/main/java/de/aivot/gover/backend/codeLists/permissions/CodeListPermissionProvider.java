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
        return "Codelisten";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(CODE_LIST_CREATE, "Codeliste erstellen", "Erlaubt das Erstellen von Codelisten."),
                PermissionEntry.of(CODE_LIST_READ, "Codeliste anzeigen", "Erlaubt das Anzeigen und Auflisten von Codelisten."),
                PermissionEntry.of(CODE_LIST_UPDATE, "Codeliste bearbeiten", "Erlaubt das Bearbeiten von Codelisten."),
                PermissionEntry.of(CODE_LIST_DELETE, "Codeliste löschen", "Erlaubt das Löschen von Codelisten."),
                PermissionEntry.of(CODE_LIST_EXPORT, "Codeliste exportieren", "Erlaubt das Exportieren von Codelisten.")
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.System;
    }
}
