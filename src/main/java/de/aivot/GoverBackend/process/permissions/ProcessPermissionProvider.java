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
                PermissionEntry.of(PROCESS_DEFINITION_CREATE, "Prozess anlegen", "Erlaubt das Anlegen neuer Prozesse."),
                PermissionEntry.of(PROCESS_DEFINITION_READ, "Prozess anzeigen", "Erlaubt das Anzeigen von Prozessen."),
                PermissionEntry.of(PROCESS_DEFINITION_UPDATE, "Prozess bearbeiten", "Erlaubt das Bearbeiten bestehender Prozesse."),
                PermissionEntry.of(PROCESS_DEFINITION_DELETE, "Prozess löschen", "Erlaubt das Löschen von Prozessen."),
                PermissionEntry.of(PROCESS_DEFINITION_AUDIT, "Prozess-Audit einsehen", "Erlaubt das Einsehen des Audit-Logs eines Prozesses."),
                PermissionEntry.of(PROCESS_DEFINITION_PUBLISH_TEST, "Prozess als Test veröffentlichen", "Erlaubt das Veröffentlichen eines Prozesses als Testversion."),
                PermissionEntry.of(PROCESS_DEFINITION_PUBLISH_LOCAL, "Prozess lokal veröffentlichen", "Erlaubt das Veröffentlichen eines Prozesses im lokalen System."),
                PermissionEntry.of(PROCESS_DEFINITION_PUBLISH_STORE, "Prozess im Store veröffentlichen", "Erlaubt das Veröffentlichen eines Prozesses im zentralen Store."),
                PermissionEntry.of(PROCESS_INSTANCE_TRIGGER, "Vorgang starten", "Erlaubt das Starten neuer Vorgänge."),
                PermissionEntry.of(PROCESS_INSTANCE_READ, "Vorgang anzeigen", "Erlaubt das Anzeigen von Vorgängen."),
                PermissionEntry.of(PROCESS_INSTANCE_UPDATE, "Vorgang bearbeiten", "Erlaubt das Bearbeiten von Vorgängen."),
                PermissionEntry.of(PROCESS_INSTANCE_DELETE, "Vorgang löschen", "Erlaubt das Löschen von Vorgängen."),
                PermissionEntry.of(PROCESS_INSTANCE_PAUSE_RESUME, "Vorgang pausieren/fortsetzen", "Erlaubt das Pausieren und Fortsetzen von Vorgängen."),
                PermissionEntry.of(PROCESS_INSTANCE_EDIT_DATA, "Geschützte Vorgangsdaten bearbeiten", "Erlaubt das Bearbeiten geschützter Vorgangsdaten."),
                PermissionEntry.of(PROCESS_INSTANCE_REASSIGN, "Vorgang neu zuweisen", "Erlaubt das Zuweisen eines Vorgangs an andere Benutzer:innen."),
                PermissionEntry.of(PROCESS_INSTANCE_COMMUNICATION_INTERNAL, "Interne Kommunikation", "Erlaubt die interne Kommunikation innerhalb eines Vorgangs."),
                PermissionEntry.of(PROCESS_INSTANCE_COMMUNICATION_EXTERNAL, "Externe Kommunikation", "Erlaubt die externe Kommunikation aus einem Vorgang heraus."),
                PermissionEntry.of(PROCESS_INSTANCE_EDIT_TASK, "Aufgaben bearbeiten", "Erlaubt das Bearbeiten von Aufgaben innerhalb eines Vorgangs."),
                PermissionEntry.of(PROCESS_INSTANCE_MIGRATE, "Vorgang migrieren", "Erlaubt die Migration von Vorgängen auf eine neue Version."),
        };
    }

    @Override
    public PermissionScope getScope() {
        return PermissionScope.Domain;
    }
}
