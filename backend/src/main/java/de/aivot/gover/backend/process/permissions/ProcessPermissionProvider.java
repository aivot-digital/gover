package de.aivot.gover.backend.process.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProcessPermissionProvider implements PermissionProvider {
    public static final String PROCESS_DEFINITION_CREATE = "process_definition.create";
    public static final String PROCESS_DEFINITION_READ = "process_definition.read";
    public static final String PROCESS_DEFINITION_UPDATE = "process_definition.update";
    public static final String PROCESS_DEFINITION_DELETE = "process_definition.delete";
    public static final String PROCESS_DEFINITION_AUDIT = "process_definition.audit";
    public static final String PROCESS_DEFINITION_PUBLISH_TEST = "process_definition.publish.test";
    public static final String PROCESS_DEFINITION_PUBLISH_LOCAL = "process_definition.publish.local";
    public static final String PROCESS_DEFINITION_PUBLISH_STORE = "process_definition.publish.store";

    @Deprecated
    public static final String PROCESS_INSTANCE_TRIGGER = ProcessInstancePermissionProvider.PROCESS_INSTANCE_TRIGGER;
    @Deprecated
    public static final String PROCESS_INSTANCE_READ = ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
    @Deprecated
    public static final String PROCESS_INSTANCE_UPDATE = ProcessInstancePermissionProvider.PROCESS_INSTANCE_UPDATE;
    @Deprecated
    public static final String PROCESS_INSTANCE_DELETE = ProcessInstancePermissionProvider.PROCESS_INSTANCE_DELETE;
    @Deprecated
    public static final String PROCESS_INSTANCE_EDIT_TASK = ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK;


    @Override
    public String getContextLabel() {
        return "Prozesse";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(PROCESS_DEFINITION_CREATE, "Prozess anlegen", "Erlaubt das Anlegen neuer Prozesse."),
                PermissionEntry.of(PROCESS_DEFINITION_READ, "Prozess anzeigen", "Erlaubt das Anzeigen von Prozessen."),
                PermissionEntry.of(PROCESS_DEFINITION_UPDATE, "Prozess bearbeiten", "Erlaubt das Bearbeiten bestehender Prozesse."),
                PermissionEntry.of(PROCESS_DEFINITION_DELETE, "Prozess löschen", "Erlaubt das Löschen von Prozessen."),
                PermissionEntry.of(PROCESS_DEFINITION_AUDIT, "Prozess-Audit einsehen", "Erlaubt das Einsehen des Audit-Logs eines Prozesses."),
                PermissionEntry.of(PROCESS_DEFINITION_PUBLISH_TEST, "Prozess als Test veröffentlichen", "Erlaubt das Veröffentlichen eines Prozesses als Testversion."),
                PermissionEntry.of(PROCESS_DEFINITION_PUBLISH_LOCAL, "Prozess lokal veröffentlichen", "Erlaubt das Veröffentlichen eines Prozesses im lokalen System."),
                PermissionEntry.of(PROCESS_DEFINITION_PUBLISH_STORE, "Prozess im Store veröffentlichen", "Erlaubt das Veröffentlichen eines Prozesses im zentralen Store."),
        };
    }

    @Override
    public boolean supportsDomainRoleAssignment() {
        return true;
    }

    @Nonnull
    @Override
    public List<SearchPermission> getSearchPermissions() {
        return List.of(
                new PermissionProvider.SearchPermission(
                        "process_nodes",
                        PROCESS_DEFINITION_READ
                ),
                new PermissionProvider.SearchPermission(
                        "processes",
                        PROCESS_DEFINITION_READ
                )
        );
    }
}
