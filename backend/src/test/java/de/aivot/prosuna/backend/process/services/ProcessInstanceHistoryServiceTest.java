package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.repositories.VUserSystemPermissionRepository;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessInstanceHistoryServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"instance", "system", "other-instance", "none"})
    @SuppressWarnings("unchecked")
    void authorizesBeforeReadingTasksOrResolvingATarget(String grant) throws Exception {
        var instances = mock(ProcessInstanceRepository.class);
        var system = mock(VUserSystemPermissionRepository.class);
        var tasks = mock(ProcessInstanceTaskRepository.class);
        var permission = ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ;
        if (grant.equals("system")) when(system.hasPermission("actor", permission)).thenReturn(true);
        if (grant.equals("instance") || grant.equals("other-instance")) {
            when(instances.hasPermission("actor", grant.equals("instance") ? 12L : 99L, permission)).thenReturn(true);
        }
        var permissions = new PermissionService(null, null, system, null, null, null, instances);
        var history = new ProcessInstanceHistoryService(tasks, permissions);
        when(tasks.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(0, 20)));
        if (grant.equals("instance") || grant.equals("system")) {
            assertTrue(history.getTasks("actor", 12, PageRequest.of(0, 20), "42").tasks().isEmpty());
            verify(tasks).findById(42L);
        } else {
            assertThrows(ResponseException.class, () -> history.getTasks("actor", 12, PageRequest.of(0, 20), "42"));
            verifyNoInteractions(tasks);
        }
    }
}
