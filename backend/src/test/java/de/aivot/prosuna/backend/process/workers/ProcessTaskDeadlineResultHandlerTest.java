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
import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineDefinition;
import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import java.time.Clock;
import java.time.ZoneId;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodePortResolution;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeService;
import de.aivot.prosuna.backend.process.services.ProcessNodePortResolver;
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
import de.aivot.prosuna.backend.process.services.ProcessWorkRequestService;

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
    private static final ProcessNodeDeadlineDefinition CUSTOMER = new ProcessNodeDeadlineDefinition(
            "customer", "Kundenbearbeitung", "Zeit für die Bearbeitung", new ProcessNodePort("customer_overdue", "Fristüberschreitung", "Fortsetzen"));
    private static final ProcessNodeDeadlineDefinition PREPARATION = new ProcessNodeDeadlineDefinition(
            "preparation", "Vorbereitung", "Zeit für die Vorbereitung", new ProcessNodePort("preparation_overdue", "Vorbereitung überfällig", "Fortsetzen"));
    private final ProcessService processes = mock(ProcessService.class);
    private final DepartmentService departments = mock(DepartmentService.class);
    private final Clock clock = mock(Clock.class);
    private final CommunicationService communication = mock(CommunicationService.class);
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
            .setOutputMappings(Map.of()).setDeadlines(Map.of("customer", new ProcessNodeDeadlineConfiguration(2, true)));
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L).setProcessId(10)
            .setStatus(ProcessInstanceStatus.Running).setInitialPayload(Map.of());
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(2L)
            .setProcessInstanceId(1L).setStatus(ProcessTaskStatus.Running).setDeadline(PREVIOUS_DEADLINE).setDeadlineKey("preparation");
    private final UserEntity assignee = new UserEntity().setId("staff").setFullName("Anna Beispiel");
    private final ProcessNodeExecutionResultHandler handler = new ProcessNodeExecutionResultHandler(
            assignments, mock(ProcessWorkRequestService.class), communication, mock(ProcessInstanceRepository.class), versions, tasks,
            edges, users, mail, mock(ProcessNodeRepository.class), mock(ProcessNodeDefinitionService.class),
            processes, departments, null, nodes, new BusinessTime(ZoneId.of("Europe/Berlin"), clock));

    @BeforeEach
    void setup() throws Exception {
        when(clock.withZone(any())).thenReturn(clock);
        when(clock.getZone()).thenReturn(ZoneId.of("Europe/Berlin"));
        when(clock.instant()).thenReturn(Instant.parse("2030-05-01T12:00:00Z"));
        when(versions.findById(ProcessVersionEntityId.of(10, 1)))
                .thenReturn(Optional.of(new ProcessVersionEntity().setProcessId(10).setProcessVersion(1)));
        when(definition.getOutputs()).thenReturn(List.of());
        when(nodes.resolvePorts(eq(node), eq(definition), any())).thenReturn(new ProcessNodePortResolution(
                List.of(new ProcessNodePort("success", "Weiter", "Fortsetzen")), List.of(), List.of(PREPARATION, CUSTOMER)));
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
        handle(result.setNewDeadlineKey("customer"));

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
        task.setDeadline(null).setDeadlineKey(null);
        var result = new ProcessNodeExecutionResultNoop().setNewDeadlineKey("customer");

        handle(result);
        handle(result);

        assertEquals(NEW_DEADLINE, task.getDeadline());
        verify(tasks).save(task);
        assertDeadlineEvent(null);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "preparation")
    void absentOrIdenticalDeadlineCausesNoAdditionalWrites(@Nullable String deadline) throws Exception {
        handle(new ProcessNodeExecutionResultNoop().setNewDeadlineKey(deadline));

        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
        verifyNoInteractions(tasks, events, mail);
    }

    @Test
    void unconfiguredPhaseClearsThePreviousDeadlineAndLogsItsKey() throws Exception {
        task.setDeadlineStrict(true);
        node.setDeadlines(Map.of());
        handle(new ProcessNodeExecutionResultNoop().setNewDeadlineKey("customer"));
        handle(new ProcessNodeExecutionResultNoop().setNewDeadlineKey("customer"));
        assertNull(task.getDeadline());
        assertNull(task.getDeadlineKey());
        assertFalse(task.getDeadlineStrict());
        verify(tasks).save(task);
        var captor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(events).save(captor.capture());
        var event = captor.getValue();
        assertTrue(event.getHistoryRelevant());
        assertEquals("preparation", event.getDetails().get("previousDeadlineKey"));
        assertNull(event.getDetails().get("newDeadlineKey"));
        assertNull(event.getDetails().get("newDeadline"));
        assertTrue(event.getMessage().contains("aufgehoben"));
    }

    @Test
    void computesDaysFromPhaseEntryAcrossDaylightSavingTimeAndDoesNotExtendOnReselection() throws Exception {
        when(clock.instant()).thenReturn(Instant.parse("2030-03-30T11:00:00Z"));
        handle(new ProcessNodeExecutionResultNoop().setNewDeadlineKey("customer"));
        assertEquals(Instant.parse("2030-04-01T10:00:00Z"), task.getDeadline());
        when(clock.instant()).thenReturn(Instant.parse("2030-04-10T10:00:00Z"));
        handle(new ProcessNodeExecutionResultNoop().setNewDeadlineKey("customer"));
        assertEquals(Instant.parse("2030-04-01T10:00:00Z"), task.getDeadline());
        assertEquals("customer", task.getDeadlineKey());
        verify(tasks).save(task);
        verify(events).save(any());
    }

    @Test
    void unknownDeadlineIsRejectedBeforeCommunicationOrAssignment() {
        var result = ProcessNodeExecutionResultTaskAssigned.of("staff").setNewDeadlineKey("unknown")
                .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest("customer",
                        CommunicationMessage.of("Betreff", "Nachricht", "Nachricht", List.of(), List.of())));
        assertThrows(ProcessNodeExecutionException.class, () -> handle(result));
        verifyNoInteractions(tasks, events, communication, assignments, mail);
        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
    }

    @Test
    void failedDispatchPreservesThePreparationDeadline() throws Exception {
        when(processes.retrieve(10)).thenReturn(Optional.of(new ProcessEntity().setId(10).setDepartmentId(20)));
        when(departments.retrieve(20)).thenReturn(Optional.of(new DepartmentEntity().setId(20)));
        when(communication.sendMessage(any(), any())).thenThrow(new CommunicationException("Delivery failed"));
        var result = ProcessNodeExecutionResultTaskAssignedCustomer.of("customer").setNewDeadlineKey("customer")
                .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest("customer",
                        CommunicationMessage.of("Betreff", "Nachricht", "Nachricht", List.of(), List.of())));
        assertThrows(ProcessNodeExecutionException.class, () -> handle(result));
        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
        assertEquals("preparation", task.getDeadlineKey());
        verifyNoInteractions(events, mail);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bothSoftAndHardPhasesReceiveADueDate(boolean strict) throws Exception {
        task.setDeadlineStrict(!strict);
        node.setDeadlines(Map.of("customer", new ProcessNodeDeadlineConfiguration(2, strict)));
        handle(new ProcessNodeExecutionResultNoop().setNewDeadlineKey("customer"));
        assertEquals(NEW_DEADLINE, task.getDeadline());
        assertEquals("customer", task.getDeadlineKey());
        // Only hard phases are picked up by the deadline worker.
        assertEquals(strict, task.getDeadlineStrict());
    }

    @Test
    void persistsDeadlineBeforeAssignmentEmailAndLogsItOnlyOnce() throws Exception {
        doAnswer(invocation -> {
            assertEquals(NEW_DEADLINE, task.getDeadline());
            verify(tasks).save(task);
            return null;
        }).when(mail).sendAssigned(isNull(), same(assignee), same(instance), same(task), same(node), same(definition), eq(false));

        handle(ProcessNodeExecutionResultTaskAssigned.of("staff").setNewDeadlineKey("customer"));

        verify(mail).sendAssigned(null, assignee, instance, task, node, definition, false);
        verify(tasks).save(task);
        assertDeadlineEvent(PREVIOUS_DEADLINE);
    }

    @ParameterizedTest
    @CsvSource({"Running, Completed", "Completed, Running", "Aborted, Running"})
    void ignoresLateDeadlineChanges(ProcessInstanceStatus instanceStatus, ProcessTaskStatus taskStatus) throws Exception {
        instance.setStatus(instanceStatus);
        task.setStatus(taskStatus);

        handle(new ProcessNodeExecutionResultNoop().setNewDeadlineKey("customer"));

        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
        verifyNoInteractions(tasks, events, mail);
    }

    @Test
    void rejectedAssignmentDoesNotChangeDeadlineOrLogSuccess() throws Exception {
        doThrow(ResponseException.forbidden()).when(assignments).saveRuntimeAssignment(task, "staff");

        assertThrows(ProcessNodeExecutionExceptionInvalidAssignment.class,
                () -> handle(ProcessNodeExecutionResultTaskAssigned.of("staff").setNewDeadlineKey("customer")));

        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
        verifyNoInteractions(tasks, events, mail);
    }

    @Test
    void rejectedCompletionDoesNotChangeDeadlineOrLogSuccess() {
        assertThrows(ProcessNodeExecutionException.class,
                () -> handle(new ProcessNodeExecutionResultTaskCompleted("missing").setNewDeadlineKey("customer")));

        assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
        verifyNoInteractions(tasks, events, mail);
    }

    @Test
    void completionRequiresAHardDeadlineForTheActivePhase() throws Exception {
        try (var validators = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var resolver = new ProcessNodePortResolver(validators.getValidator());
            when(definition.getPorts(any())).thenReturn(List.of());
            when(definition.getDeadlines(any())).thenReturn(List.of(PREPARATION, CUSTOMER));
            when(nodes.resolvePorts(eq(node), eq(definition), any())).thenAnswer(invocation ->
                    resolver.resolve(node, definition, invocation.<AuthoredElementValues>getArgument(2)));
            when(edges.findByFromNodeIdAndViaPort(3, "customer_overdue"))
                    .thenReturn(Optional.of(new ProcessEdgeEntity(1, 10, 1, 3, 4, "customer_overdue")));
            var result = new ProcessNodeExecutionResultTaskCompleted("customer_overdue");

            assertThrows(ProcessNodeExecutionException.class, () -> handle(result));
            assertEquals(ProcessTaskStatus.Running, task.getStatus());
            task.setDeadlineKey("customer").setDeadline(null);
            assertThrows(ProcessNodeExecutionException.class, () -> handle(result));
            task.setDeadline(PREVIOUS_DEADLINE);
            node.setDeadlines(Map.of("customer", new ProcessNodeDeadlineConfiguration(2, false)));
            assertThrows(ProcessNodeExecutionException.class, () -> handle(result));
            verifyNoInteractions(tasks, events, edges);

            node.setDeadlines(Map.of("customer", new ProcessNodeDeadlineConfiguration(2, true)));
            handle(result);
            assertEquals(ProcessTaskStatus.Completed, task.getStatus());
            assertEquals("customer", task.getDeadlineKey());
            assertEquals(PREVIOUS_DEADLINE, task.getDeadline());
            verify(edges).findByFromNodeIdAndViaPort(3, "customer_overdue");
        }
    }

    @Test
    void resolvesRuntimePortsFromTheExecutingConfigurationWithoutDerivingItAgain() throws Exception {
        var configuration = new AuthoredElementValues();
        var result = new ProcessNodeExecutionResultTaskCompleted("success").setNewDeadlineKey("customer");

        handler.handleResult(new ProcessNodeExecutionLogger(1L, 2L, null, null, events), null,
                definition, configuration, node, instance, task, null, result);

        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
        verify(nodes, times(2)).resolvePorts(same(node), same(definition), same(configuration));
        verify(nodes, never()).resolvePorts(any(), any());
    }

    @Test
    void failedDeadlineSaveDoesNotLogSuccess() {
        when(tasks.save(task)).thenThrow(new IllegalStateException("Persistence failed"));

        assertThrows(IllegalStateException.class,
                () -> handle(new ProcessNodeExecutionResultNoop().setNewDeadlineKey("customer")));

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
        assertEquals("customer", task.getDeadlineKey());
        assertEquals("customer", event.getDetails().get("newDeadlineKey"));
        assertEquals(previousDeadline == null ? null : "preparation", event.getDetails().get("previousDeadlineKey"));
        assertTrue(event.getMessage().contains(ExecutionSummaryMarkdown.timestamp(NEW_DEADLINE)));
        assertTrue(event.getMessage().contains("Kundenbearbeitung"));
    }
}
