package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.services.ProcessInstanceMailService;
import de.aivot.prosuna.backend.mail.services.ProcessTaskMailService;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.InstanceAssignmentActionNodeV1;
import de.aivot.prosuna.backend.plugins.core.v1.nodes.actions.InstanceUnassignmentActionNodeV1;
import de.aivot.prosuna.backend.process.entities.ProcessEdgeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceAssigned;
import de.aivot.prosuna.backend.process.repositories.ProcessEdgeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InstanceAssignmentResultHandlerTest {
    private final ProcessAssignmentService assignments = mock(ProcessAssignmentService.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessEdgeRepository edges = mock(ProcessEdgeRepository.class);
    private final UserService users = mock(UserService.class);
    private final ProcessInstanceMailService instanceMail = mock(ProcessInstanceMailService.class);
    private final ProcessNodeExecutionLogger logger = mock(ProcessNodeExecutionLogger.class);
    private final InstanceAssignmentActionNodeV1 provider = new InstanceAssignmentActionNodeV1(
            mock(AssignmentContextAssigneeResolverService.class), assignments);
    private final InstanceUnassignmentActionNodeV1 unassignmentProvider = new InstanceUnassignmentActionNodeV1();
    private final ProcessNodeExecutionResultHandler handler = new ProcessNodeExecutionResultHandler(
            assignments, rabbit, mock(CommunicationService.class), instances, tasks, edges, users,
            mock(ProcessTaskMailService.class), mock(ProcessNodeRepository.class),
            mock(ProcessNodeDefinitionService.class), mock(ProcessService.class),
            mock(DepartmentService.class), instanceMail);

    @BeforeEach
    void setUp() throws Exception {
        when(users.retrieve("previous")).thenReturn(Optional.of(user("previous")));
        when(users.retrieve("recipient")).thenReturn(Optional.of(user("recipient")));
        when(edges.findByFromNodeIdAndViaPort(12, "success"))
                .thenReturn(Optional.of(new ProcessEdgeEntity(1, 42, 3, 12, 13, "success")));
    }

    @Test
    void assignmentCompletesTaskMapsOutputAndQueuesNextNode() throws Exception {
        var instance = instance();
        var task = task();
        var node = node();
        node.setOutputMappings(Map.of("assignedUserId", "responsibleUserId"));

        handler.handleResult(logger, null, provider, node, instance, task, null, result("recipient"));

        assertEquals("recipient", instance.getAssignedUserId());
        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
        assertEquals("recipient", task.getNodeData().get("assignedUserId"));
        assertEquals("recipient", task.getProcessData().get("responsibleUserId"));
        verify(assignments).requireRuntimeInstanceAssignee(99L, "recipient");
        verify(instances).save(instance);
        verify(tasks).save(task);
        verify(rabbit).convertAndSend(eq(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE),
                any(ProcessWorker.DoWorkWorkerPayload.class));
        verify(instanceMail).sendAssigned(isNull(), any(UserEntity.class), same(instance), eq(true));
        verify(instanceMail).sendUnassigned(isNull(), any(UserEntity.class), same(instance), eq(true));
    }

    @Test
    void invalidOutgoingPathDoesNotChangeAssignment() {
        when(edges.findByFromNodeIdAndViaPort(12, "success")).thenReturn(Optional.empty());
        var instance = instance();

        assertThrows(ProcessNodeExecutionExceptionBrokenImplementation.class,
                () -> handler.handleResult(logger, null, provider, node(), instance, task(), null, result("recipient")));

        assertEquals("previous", instance.getAssignedUserId());
        verifyNoInteractions(instances, tasks, rabbit, instanceMail);
    }

    @Test
    void revokedRecipientAccessDoesNotChangeAssignment() throws Exception {
        doThrow(ResponseException.badRequest("Kein Zugriff"))
                .when(assignments).requireRuntimeInstanceAssignee(99L, "recipient");
        var instance = instance();

        assertThrows(ProcessNodeExecutionExceptionInvalidAssignment.class,
                () -> handler.handleResult(logger, null, provider, node(), instance, task(), null, result("recipient")));

        assertEquals("previous", instance.getAssignedUserId());
        verifyNoInteractions(instances, tasks, rabbit, instanceMail);
    }

    @Test
    void invalidOutputMappingDoesNotChangeAssignment() {
        var instance = instance();
        var node = node();
        node.setOutputMappings(Map.of("assignedUserId", "case..owner"));

        assertThrows(IllegalStateException.class,
                () -> handler.handleResult(logger, null, provider, node, instance, task(), null, result("recipient")));

        assertEquals("previous", instance.getAssignedUserId());
        verifyNoInteractions(instances, tasks, rabbit, instanceMail);
    }

    @Test
    void assigningExistingRecipientStillCompletesWithoutMail() throws Exception {
        var instance = instance();
        var task = task();

        handler.handleResult(logger, null, provider, node(), instance, task, null, result("previous"));

        assertEquals("previous", instance.getAssignedUserId());
        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
        verifyNoInteractions(instanceMail);
    }

    @Test
    void removingAssignmentCompletesTaskAndNotifiesPreviousRecipient() throws Exception {
        var instance = instance();
        var task = task();

        handler.handleResult(logger, null, unassignmentProvider, node(), instance, task, null, clearResult());

        assertNull(instance.getAssignedUserId());
        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
        assertNotNull(task.getFinished());
        assertEquals(Map.of("case", "data"), task.getProcessData());
        verify(instances).save(instance);
        verify(tasks).save(task);
        verify(rabbit).convertAndSend(eq(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE),
                any(ProcessWorker.DoWorkWorkerPayload.class));
        verify(instanceMail).sendUnassigned(isNull(), any(UserEntity.class), same(instance), eq(false));
        verify(instanceMail, never()).sendAssigned(any(), any(), any(), anyBoolean());
        verify(assignments, never()).requireRuntimeInstanceAssignee(anyLong(), anyString());
    }

    @Test
    void removingAbsentAssignmentStillCompletesWithoutNotification() throws Exception {
        var instance = instance().setAssignedUserId(null);
        var task = task();

        handler.handleResult(logger, null, unassignmentProvider, node(), instance, task, null, clearResult());

        assertNull(instance.getAssignedUserId());
        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
        verify(rabbit).convertAndSend(eq(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE),
                any(ProcessWorker.DoWorkWorkerPayload.class));
        verifyNoInteractions(instanceMail);
    }

    private static ProcessNodeExecutionResultInstanceAssigned result(String userId) {
        var result = ProcessNodeExecutionResultInstanceAssigned.assignAndContinue(userId, "success");
        result.setNodeData(Map.of("assignedUserId", userId));
        result.setProcessData(Map.of("case", "data"));
        return result;
    }

    private static ProcessNodeExecutionResultInstanceAssigned clearResult() {
        var result = ProcessNodeExecutionResultInstanceAssigned.clear().setViaPort("success");
        result.setProcessData(Map.of("case", "data"));
        return result;
    }

    private static ProcessInstanceEntity instance() {
        return new ProcessInstanceEntity().setId(99L).setAssignedUserId("previous")
                .setStatus(ProcessInstanceStatus.Running);
    }

    private static ProcessInstanceTaskEntity task() {
        return new ProcessInstanceTaskEntity().setId(17L).setProcessInstanceId(99L)
                .setStatus(ProcessTaskStatus.Running).setProcessData(Map.of("case", "data"));
    }

    private static ProcessNodeEntity node() {
        return new ProcessNodeEntity().setId(12).setName("Vorgang zuweisen")
                .setOutputMappings(Map.of());
    }

    private static UserEntity user(String id) {
        return new UserEntity().setId(id).setFullName(id);
    }
}
