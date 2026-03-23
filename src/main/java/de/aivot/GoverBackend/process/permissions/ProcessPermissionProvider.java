package de.aivot.GoverBackend.process.permissions;

import de.aivot.GoverBackend.permissions.enums.PermissionScope;
import de.aivot.GoverBackend.permissions.models.PermissionEntry;
import de.aivot.GoverBackend.permissions.models.PermissionProvider;
import org.springframework.stereotype.Component;

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
    public static final String PROCESS_INSTANCE_TRIGGER = "process_instance.trigger";
    public static final String PROCESS_INSTANCE_READ = "process_instance.read";
    public static final String PROCESS_INSTANCE_UPDATE = "process_instance.update";
    public static final String PROCESS_INSTANCE_DELETE = "process_instance.delete";
    public static final String PROCESS_INSTANCE_PAUSE_RESUME = "process_instance.pause_resume";
    public static final String PROCESS_INSTANCE_EDIT_DATA = "process_instance.edit_data";
    public static final String PROCESS_INSTANCE_REASSIGN = "process_instance.reassign";
    public static final String PROCESS_INSTANCE_COMMUNICATION_INTERNAL = "process_instance.communication.internal";
    public static final String PROCESS_INSTANCE_COMMUNICATION_EXTERNAL = "process_instance.communication.external";
    public static final String PROCESS_INSTANCE_EDIT_TASK = "process_instance.edit_task";
    public static final String PROCESS_INSTANCE_MIGRATE = "process_instance.migrate";

    @Override
    public String getContextLabel() {
        return "Prozesse";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(PROCESS_DEFINITION_CREATE, "Prozessdefinition anlegen", "Erlaubt das Anlegen neuer Prozessdefinitionen."),
                PermissionEntry.of(PROCESS_DEFINITION_READ, "Prozessdefinition anzeigen", "Erlaubt das Anzeigen von Prozessdefinitionen."),
                PermissionEntry.of(PROCESS_DEFINITION_UPDATE, "Prozessdefinition bearbeiten", "Erlaubt das Bearbeiten bestehender Prozessdefinitionen."),
                PermissionEntry.of(PROCESS_DEFINITION_DELETE, "Prozessdefinition löschen", "Erlaubt das Löschen von Prozessdefinitionen."),
                PermissionEntry.of(PROCESS_DEFINITION_AUDIT, "Prozessdefinition Audit einsehen", "Erlaubt das Einsehen des Audit-Logs einer Prozessdefinition."),
                PermissionEntry.of(PROCESS_DEFINITION_PUBLISH_TEST, "Prozessdefinition als Test veröffentlichen", "Erlaubt das Veröffentlichen einer Prozessdefinition als Testversion."),
                PermissionEntry.of(PROCESS_DEFINITION_PUBLISH_LOCAL, "Prozessdefinition lokal veröffentlichen", "Erlaubt das Veröffentlichen einer Prozessdefinition im lokalen System."),
                PermissionEntry.of(PROCESS_DEFINITION_PUBLISH_STORE, "Prozessdefinition im Store veröffentlichen", "Erlaubt das Veröffentlichen einer Prozessdefinition im zentralen Store."),
                PermissionEntry.of(PROCESS_INSTANCE_TRIGGER, "Prozessinstanz starten", "Erlaubt das Starten neuer Prozessinstanzen."),
                PermissionEntry.of(PROCESS_INSTANCE_READ, "Prozessinstanz anzeigen", "Erlaubt das Anzeigen von Prozessinstanzen."),
                PermissionEntry.of(PROCESS_INSTANCE_UPDATE, "Prozessinstanz bearbeiten", "Erlaubt das Bearbeiten von Prozessinstanzen."),
                PermissionEntry.of(PROCESS_INSTANCE_DELETE, "Prozessinstanz löschen", "Erlaubt das Löschen von Prozessinstanzen."),
                PermissionEntry.of(PROCESS_INSTANCE_PAUSE_RESUME, "Prozessinstanz pausieren/fortsetzen", "Erlaubt das Pausieren und Fortsetzen von Prozessinstanzen."),
                PermissionEntry.of(PROCESS_INSTANCE_EDIT_DATA, "Prozessdaten bearbeiten", "Erlaubt das Bearbeiten der Daten einer Prozessinstanz."),
                PermissionEntry.of(PROCESS_INSTANCE_REASSIGN, "Prozessinstanz neu zuweisen", "Erlaubt das Zuweisen einer Prozessinstanz an andere Benutzer."),
                PermissionEntry.of(PROCESS_INSTANCE_COMMUNICATION_INTERNAL, "Interne Kommunikation", "Erlaubt die interne Kommunikation innerhalb einer Prozessinstanz."),
                PermissionEntry.of(PROCESS_INSTANCE_COMMUNICATION_EXTERNAL, "Externe Kommunikation", "Erlaubt die externe Kommunikation aus einer Prozessinstanz heraus."),
                PermissionEntry.of(PROCESS_INSTANCE_EDIT_TASK, "Aufgaben bearbeiten", "Erlaubt das Bearbeiten von Aufgaben innerhalb einer Prozessinstanz."),
                PermissionEntry.of(PROCESS_INSTANCE_MIGRATE, "Prozessinstanz migrieren", "Erlaubt die Migration von Prozessinstanzen auf eine neue Version."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.Domain;
    }
}
