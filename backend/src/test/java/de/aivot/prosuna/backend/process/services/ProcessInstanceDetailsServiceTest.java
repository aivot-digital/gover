package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.repositories.DepartmentRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.repositories.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessInstanceDetailsServiceTest {
    private final PermissionService permissions = mock(PermissionService.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessRepository processes = mock(ProcessRepository.class);
    private final DepartmentRepository departments = mock(DepartmentRepository.class);
    private final ProcessNodeRepository nodes = mock(ProcessNodeRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessNodeDefinitionService definitions = mock(ProcessNodeDefinitionService.class);
    private final ProcessInstanceDetailsService service = new ProcessInstanceDetailsService(
            permissions, instances, processes, departments, nodes, tasks, definitions);

    @Test
    void taskDetailsRequireReadAccessToTheActualOwningInstance() throws ResponseException {
        when(tasks.findById(8L)).thenReturn(Optional.of(new ProcessInstanceTaskEntity().setProcessInstanceId(18L)));
        doThrow(ResponseException.forbidden()).when(permissions).requireProcessInstancePermission(
                "user", 18L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        assertThrows(ResponseException.class, () -> service.retrieveTask("user", 8L));
        verifyNoInteractions(instances, processes, departments, nodes, definitions);
    }

    @Test
    void taskDetailsExposeOnlyDisplayMetadataWithoutRequiringModelAccess() throws ResponseException {
        var task = new ProcessInstanceTaskEntity().setId(8L).setProcessInstanceId(17L).setProcessId(3).setProcessNodeId(4);
        when(tasks.findById(8L)).thenReturn(Optional.of(task));
        when(instances.findById(17L)).thenReturn(Optional.of(new ProcessInstanceEntity().setId(17L)));
        when(processes.findById(3)).thenReturn(Optional.of(new ProcessEntity().setId(3).setInternalTitle("Prüfung")));
        var node = new ProcessNodeEntity().setName("Unterlagen prüfen").setDescription("Nachweise prüfen");
        when(nodes.findById(4)).thenReturn(Optional.of(node));
        var definition = mock(de.aivot.prosuna.backend.process.models.ProcessNodeDefinition.class);
        when(definition.getKey()).thenReturn("test.manual");
        when(definition.getComponentKey()).thenReturn("manual");
        when(definition.getName()).thenReturn("Manuelle Aufgabe");
        when(definition.getAbstract()).thenReturn("Allgemeine Prüfung");
        when(definitions.getProcessNodeDefinition(node)).thenReturn(Optional.of(definition));

        var result = service.retrieveTask("user", 8L);
        assertSame(task, result.task());
        assertEquals("Prüfung", result.process().internalTitle());
        assertEquals("Unterlagen prüfen", result.node().name());
        assertEquals("Manuelle Aufgabe", result.provider().name());
        var json = de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils.createMapper().valueToTree(result);
        assertEquals(java.util.Set.of("id", "internalTitle"), json.get("process").propertyNames());
        assertEquals(java.util.Set.of("name", "description"), json.get("node").propertyNames());
        verify(permissions).requireProcessInstancePermission("user", 17L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        verifyNoMoreInteractions(permissions);
    }

    @Test
    void rejectsAnotherInstanceBeforeReadingItsData() throws ResponseException {
        doThrow(ResponseException.forbidden()).when(permissions).requireProcessInstancePermission(
                "user", 18L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        assertThrows(ResponseException.class, () -> service.retrieve("user", 18L));
        verifyNoInteractions(instances, processes, departments, nodes, tasks, definitions);
    }

    @Test
    void returnsInstanceMetadataAndAllActiveTasksWithoutProcessModelPermission() throws ResponseException {
        var instance = new ProcessInstanceEntity().setId(17L).setProcessId(3).setInitialNodeId(4)
                .setAssignedUserId("instance-owner").setStatusOverride("Vorgang wird geprüft");
        var process = new ProcessEntity().setId(3).setDepartmentId(6).setInternalTitle("Prüfung");
        var trigger = new ProcessNodeEntity().setId(4).setName("Eingang");
        when(instances.findById(17L)).thenReturn(Optional.of(instance));
        when(processes.findById(3)).thenReturn(Optional.of(process));
        when(departments.findById(6)).thenReturn(Optional.of(new DepartmentEntity().setName("Fachbereich")));
        when(nodes.findById(4)).thenReturn(Optional.of(trigger));
        when(tasks.findAllByProcessInstanceId(17L)).thenReturn(Arrays.stream(ProcessTaskStatus.values())
                .map(status -> new ProcessInstanceTaskEntity().setId((long) status.ordinal() + 1)
                        .setProcessNodeId(4).setStatus(status).setStatusOverride("Unterlagen angefordert")
                        .setAssignedUserId("task-assignee").setDeadline(java.time.Instant.parse("2026-10-01T08:00:00Z"))).toList());

        var result = service.retrieve("user", 17L);

        assertEquals("Prüfung", result.processName());
        assertEquals("Fachbereich", result.departmentName());
        assertEquals("Eingang", result.triggerName());
        assertEquals(4, result.activeTasks().size());
        assertTrue(result.activeTasks().stream().allMatch(task -> java.time.Instant.parse("2026-10-01T08:00:00Z").equals(task.deadline())));
        assertEquals("instance-owner", result.instance().getAssignedUserId());
        assertEquals("Vorgang wird geprüft", result.instance().getStatusOverride());
        assertTrue(result.activeTasks().stream().allMatch(task -> "Unterlagen angefordert".equals(task.statusOverride())
                && "task-assignee".equals(task.assignedUserId())));
        assertTrue(result.activeTasks().stream().noneMatch(task -> task.status() == ProcessTaskStatus.Completed
                || task.status() == ProcessTaskStatus.Failed || task.status() == ProcessTaskStatus.Restarted));
        verify(permissions).requireProcessInstancePermission("user", 17L, ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ);
        verifyNoMoreInteractions(permissions);
    }
}
