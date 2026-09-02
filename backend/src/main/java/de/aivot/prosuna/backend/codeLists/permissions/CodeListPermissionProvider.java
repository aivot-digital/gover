package de.aivot.prosuna.backend.codeLists.permissions;

import de.aivot.prosuna.backend.permissions.models.PermissionEntry;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "code_lists",
                CODE_LIST_READ
        ));
    }
}
