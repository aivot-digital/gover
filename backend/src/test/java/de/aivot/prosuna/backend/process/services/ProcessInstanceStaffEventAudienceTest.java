package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.staffEvents.models.StaffEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessInstanceStaffEventAudienceTest {
    @Test
    void sendsTheChangeOfAnInstanceToTheUsersWhoMayReadIt() {
        var permissions = mock(PermissionService.class);
        var connected = List.of("reader", "other");
        when(permissions.filterUsersWithProcessInstancePermission(connected, 7L, PROCESS_INSTANCE_READ))
                .thenReturn(Set.of("reader"));
        var audience = new ProcessInstanceStaffEventAudience(permissions);

        assertEquals("process-instance", audience.type());
        assertEquals(Set.of("reader"), audience.filter(new StaffEvent("process-instance", "7", null), connected));
    }
}
