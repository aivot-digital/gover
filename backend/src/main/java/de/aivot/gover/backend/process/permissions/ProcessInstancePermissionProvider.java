package de.aivot.gover.backend.process.permissions;

import de.aivot.gover.backend.permissions.models.PermissionEntry;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProcessInstancePermissionProvider implements PermissionProvider {
    public static final String PROCESS_INSTANCE_TRIGGER = "process_instance.trigger";
    public static final String PROCESS_INSTANCE_READ = "process_instance.read";
    public static final String PROCESS_INSTANCE_UPDATE = "process_instance.update";
    public static final String PROCESS_INSTANCE_DELETE = "process_instance.delete";
    public static final String PROCESS_INSTANCE_PAUSE_RESUME = "process_instance.pause_resume";
    public static final String PROCESS_INSTANCE_REASSIGN = "process_instance.reassign";
    public static final String PROCESS_INSTANCE_COMMUNICATION_INTERNAL = "process_instance.communication.internal";
    public static final String PROCESS_INSTANCE_COMMUNICATION_EXTERNAL = "process_instance.communication.external";
    public static final String PROCESS_INSTANCE_EDIT_TASK = "process_instance.edit_task";
    public static final String PROCESS_INSTANCE_MIGRATE = "process_instance.migrate";

    @Override
    public String getContextLabel() {
        return "Vorgänge";
    }

    @Override
    public PermissionEntry[] getPermissions() {
        return new PermissionEntry[]{
                PermissionEntry.of(PROCESS_INSTANCE_TRIGGER, "Vorgang starten", "Erlaubt das Starten neuer Vorgänge."),
                PermissionEntry.of(PROCESS_INSTANCE_READ, "Vorgang anzeigen", "Erlaubt das Anzeigen von Vorgängen."),
                PermissionEntry.of(PROCESS_INSTANCE_UPDATE, "Vorgang bearbeiten", "Erlaubt das Bearbeiten von Vorgängen."),
                PermissionEntry.of(PROCESS_INSTANCE_DELETE, "Vorgang löschen", "Erlaubt das Löschen von Vorgängen."),
                PermissionEntry.of(PROCESS_INSTANCE_PAUSE_RESUME, "Vorgang pausieren/fortsetzen", "Erlaubt das Pausieren und Fortsetzen von Vorgängen."),
                PermissionEntry.of(PROCESS_INSTANCE_REASSIGN, "Vorgang neu zuweisen", "Erlaubt das Zuweisen eines Vorgangs an andere Benutzer:innen."),
                PermissionEntry.of(PROCESS_INSTANCE_COMMUNICATION_INTERNAL, "Interne Kommunikation", "Erlaubt die interne Kommunikation innerhalb eines Vorgangs."),
                PermissionEntry.of(PROCESS_INSTANCE_COMMUNICATION_EXTERNAL, "Externe Kommunikation", "Erlaubt die externe Kommunikation aus einem Vorgang heraus."),
                PermissionEntry.of(PROCESS_INSTANCE_EDIT_TASK, "Aufgaben bearbeiten", "Erlaubt das Bearbeiten von Aufgaben innerhalb eines Vorgangs."),
                PermissionEntry.of(PROCESS_INSTANCE_MIGRATE, "Vorgang migrieren", "Erlaubt die Migration von Vorgängen auf eine neue Version."),
        };
    }

    @Override
    public boolean supportsDomainRoleAssignment() {
        return true;
    }

    @Nonnull
    @Override
    public Optional<SearchPermission> getSearchPermission() {
        return Optional.of(new PermissionProvider.SearchPermission(
                "process_instances",
                PROCESS_INSTANCE_READ
        ));
    }
}
