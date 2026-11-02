package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceStaffEventListener;
import de.aivot.prosuna.backend.staffEvents.models.StaffEvent;
import de.aivot.prosuna.backend.staffEvents.models.StaffEventAudience;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;

/** Sends the changes of a process instance only to the users who may read it. */
@Component
public class ProcessInstanceStaffEventAudience implements StaffEventAudience {
    private final PermissionService permissions;

    public ProcessInstanceStaffEventAudience(@Nonnull PermissionService permissions) {
        this.permissions = permissions;
    }

    @Nonnull
    @Override
    public String type() {
        return ProcessInstanceStaffEventListener.STAFF_EVENT_TYPE;
    }

    @Nonnull
    @Override
    public Collection<String> filter(@Nonnull StaffEvent event, @Nonnull Collection<String> userIds) {
        // An event without an instance, e.g. after a deletion, names no data and only lets the lists reload.
        if (event.scope() == null) {
            return userIds;
        }
        return permissions.filterUsersWithProcessInstancePermission(userIds, Long.valueOf(event.scope()),
                PROCESS_INSTANCE_READ);
    }
}
