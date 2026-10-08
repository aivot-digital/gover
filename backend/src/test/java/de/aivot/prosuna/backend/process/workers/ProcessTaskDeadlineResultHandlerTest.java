package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.services.ProcessTaskMailService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessTaskDeadlineResultHandlerTest {
    private static final Instant PREVIOUS_DEADLINE = Instant.parse("2030-05-01T10:00:00Z");
    private static final Instant NEW_DEADLINE = Instant.parse("2030-05-03T12:00:00Z");
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessInstanceHistoryEventRepository events = mock(ProcessInstanceHistoryEventRepository.class);
    private final ProcessVersionRepository versions = mock(ProcessVersionRepository.class);
    private final ProcessEdgeRepository edges = mock(ProcessEdgeRepository.class);
    private final ProcessAssignmentService assignments = mock(ProcessAssignmentService.class);
    private final ProcessNodeService nodes = mock(ProcessNodeService.class);
    private final ProcessTaskMailService mail = mock(ProcessTaskMailService.class);
    private final UserService users = mock(UserService.class);
    @SuppressWarnings("unchecked")
    private final ProcessNodeDefinition<AuthoredElementValues> definition = mock(ProcessNodeDefinition.class);
    private final ProcessNodeEntity node = new ProcessNodeEntity().setId(3).setProcessId(10).setProcessVersion(1)
            .setOutputMappings(Map.of());
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L).setProcessId(10)
            .setStatus(ProcessInstanceStatus.Running).setInitialPayload(Map.of());
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(2L)
            .setProcessInstanceId(1L).setStatus(ProcessTaskStatus.Running).setDeadline(PREVIOUS_DEADLINE);
    private final UserEntity assignee = new UserEntity().setId("staff").setFullName("Anna Beispiel");
    private final ProcessNodeExecutionResultHandler handler = new ProcessNodeExecutionResultHandler(
            assignments, mock(RabbitTemplate.class), null, mock(ProcessInstanceRepository.class), versions, tasks,
            edges, users, mail, mock(ProcessNodeRepository.class), mock(ProcessNodeDefinitionService.class),
            null, null, null, nodes);

    @BeforeEach
    void setup() throws Exception {
        when(versions.findById(ProcessVersionEntityId.of(10, 1)))
                .thenReturn(Optional.of(new ProcessVersionEntity().setProcessId(10).setProcessVersion(1)));
        when(definition.getOutputs()).thenReturn(List.of());
        when(nodes.resolvePorts(node, definition)).thenReturn(new ProcessNodePortResolution(
                List.of(new ProcessNodePort("success", "Weiter", "Fortsetzen")), List.of()));
        when(edges.findByFromNodeIdAndViaPort(3, "success"))
                .thenReturn(Optional.of(new ProcessEdgeEntity(1, 10, 1, 3, 4, "success")));
        when(users.retrieve("staff")).thenReturn(Optional.of(assignee));
        var identities = new IdentityDataMap();
        identities.put("customer", new IdentityData("session", "customer", IdentityType.Email, null, null, null,
                "person@example.test", Map.of(), null, Map.of()));
        instance.setIdentities(identities);
    }

    @ParameterizedTest
    @MethodSource("executionResults")
    void updatesDeadlineAndRecordsOneHistoryEventAcrossResultTypes(ProcessNodeExecutionResult result) throws Exception {
        handle(result.setNewDeadline(NEW_DEADLINE));

        assertEquals(NEW_DEADLINE, task.getDeadline());
        verify(tasks, atLeastOnce()).save(task);
        assertDeadlineEvent(PREVIOUS_DEADLINE);
    }

    private static Stream<ProcessNodeExecutionResult> executionResults() {
        return Stream.of(
                new ProcessNodeExecutionResultNoop(),
                new ProcessNodeExecutionResultTaskUpdated(),
                new ProcessNodeExecutionResultTaskCompleted("success"),
                new ProcessNodeExecutionResultInstanceCompleted(),
                ProcessNodeExecutionResultInstanceAssigned.clear(),
                ProcessNodeExecutionResultInstanceAssigned.clear().setViaPort("success"),
                ProcessNodeExecutionResultTaskAssigned.of("staff"),
                ProcessNodeExecutionResultTaskAssignedCustomer.of("customer"),
                new ProcessNodeExecutionResultPaymentRequested("transaction", "provider")
        );
    }

    @Test
    void setsAnInitiallyMissingDeadlineAndDoesNotRepeatTheUpdate() throws Exception {
        task.setDeadline(null);
        var result = new ProcessNodeExecutionResultNoop().setNewDeadline(NEW_DEADLINE);

        handle(result);
        handle(result);

        assertEquals(NEW_DEADLINE, task.getDeadline());
        verify(tasks).save(task);
        assertDeadlineEvent(null);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "2030-05-01T10:00:00Z")
    void absentOrIdenticalDeadlineCausesNoAdditionalWrites(@Nullable String deadline) throws Exception {
        handle(new ProcessNodeExecutionResultNoop().setNewDeadline(deadline == null ? null : Instant.parse(deadline)));

        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
        verifyNoInteractions(tasks, events, mail);
    }

    @Test
    void permitsMovingTheDeadlineIntoThePast() throws Exception {
        var past = Instant.parse("2000-01-01T00:00:00Z");
        handle(new ProcessNodeExecutionResultNoop().setNewDeadline(past));

        assertEquals(past, task.getDeadline());
        verify(tasks).save(task);
    }

    @Test
    void persistsDeadlineBeforeAssignmentEmailAndLogsItOnlyOnce() throws Exception {
        doAnswer(invocation -> {
            assertEquals(NEW_DEADLINE, task.getDeadline());
            verify(tasks).save(task);
            return null;
        }).when(mail).sendAssigned(isNull(), same(assignee), same(instance), same(task), same(node), same(definition), eq(false));

        handle(ProcessNodeExecutionResultTaskAssigned.of("staff").setNewDeadline(NEW_DEADLINE));

        verify(mail).sendAssigned(null, assignee, instance, task, node, definition, false);
        verify(tasks).save(task);
        assertDeadlineEvent(PREVIOUS_DEADLINE);
    }

    @ParameterizedTest
    @CsvSource({"Running, Completed", "Completed, Running", "Aborted, Running"})
    void ignoresLateDeadlineChanges(ProcessInstanceStatus instanceStatus, ProcessTaskStatus taskStatus) throws Exception {
        instance.setStatus(instanceStatus);
        task.setStatus(taskStatus);

        handle(new ProcessNodeExecutionResultNoop().setNewDeadline(NEW_DEADLINE));

        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
        verifyNoInteractions(tasks, events, mail);
    }

    @Test
    void rejectedAssignmentDoesNotChangeDeadlineOrLogSuccess() throws Exception {
        doThrow(ResponseException.forbidden()).when(assignments).saveRuntimeAssignment(task, "staff");

        assertThrows(ProcessNodeExecutionExceptionInvalidAssignment.class,
                () -> handle(ProcessNodeExecutionResultTaskAssigned.of("staff").setNewDeadline(NEW_DEADLINE)));

        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
        verifyNoInteractions(tasks, events, mail);
    }

    @Test
    void rejectedCompletionDoesNotChangeDeadlineOrLogSuccess() {
        assertThrows(ProcessNodeExecutionException.class,
                () -> handle(new ProcessNodeExecutionResultTaskCompleted("missing").setNewDeadline(NEW_DEADLINE)));

        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
        verifyNoInteractions(tasks, events, mail);
    }

    @Test
    void failedDeadlineSaveDoesNotLogSuccess() {
        when(tasks.save(task)).thenThrow(new IllegalStateException("Persistence failed"));

        assertThrows(IllegalStateException.class,
                () -> handle(new ProcessNodeExecutionResultNoop().setNewDeadline(NEW_DEADLINE)));

        verifyNoInteractions(events, mail);
    }

    private void handle(ProcessNodeExecutionResult result) throws ProcessNodeExecutionException {
        handler.handleResult(new ProcessNodeExecutionLogger(1L, 2L, null, null, events), null,
                definition, new AuthoredElementValues(), node, instance, task, null, result);
    }

    private void assertDeadlineEvent(@Nullable Instant previousDeadline) {
        var captor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(events, atLeastOnce()).save(captor.capture());
        var deadlineEvents = captor.getAllValues().stream()
                .filter(event -> "Frist der Aufgabe geändert".equals(event.getTitle())).toList();
        assertEquals(1, deadlineEvents.size());
        var event = deadlineEvents.getFirst();
        assertEquals(ProcessNodeExecutionLogLevel.Info, event.getLevel());
        assertFalse(event.getTechnical());
        assertTrue(event.getAudit());
        assertTrue(event.getHistoryRelevant());
        assertEquals(instance.getId(), event.getProcessInstanceId());
        assertEquals(task.getId(), event.getProcessInstanceTaskId());
        assertTrue(event.getDetails().containsKey("previousDeadline"));
        assertEquals(previousDeadline, event.getDetails().get("previousDeadline"));
        assertEquals(NEW_DEADLINE, event.getDetails().get("newDeadline"));
        assertEquals(previousDeadline == null
                ? "Für die Aufgabe wurde eine Frist bis %s gesetzt."
                        .formatted(ExecutionSummaryMarkdown.timestamp(NEW_DEADLINE))
                : "Die Frist der Aufgabe wurde von %s auf %s geändert."
                        .formatted(ExecutionSummaryMarkdown.timestamp(previousDeadline), ExecutionSummaryMarkdown.timestamp(NEW_DEADLINE)),
                event.getMessage());
    }
}
