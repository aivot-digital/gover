package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.*;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessHistoryEventsTest {
    private final List<ProcessInstanceEventEntity> events = new ArrayList<>();
    private final ProcessInstanceHistoryEventRepository eventRepository = mock(ProcessInstanceHistoryEventRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessEdgeRepository edges = mock(ProcessEdgeRepository.class);
    private final UserService users = mock(UserService.class);
    private final ProcessNodeDefinition<?> definition = mock(ProcessNodeDefinition.class);
    private final UserEntity actor = new UserEntity().setId("actor-id").setFullName("Ada Beispiel");
    private final ProcessNodeEntity node = new ProcessNodeEntity().setId(3).setName("Prüfung").setOutputMappings(Map.of());
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L)
            .setStatus(ProcessInstanceStatus.Running).setInitialPayload(Map.of()).setIdentities(new IdentityDataMap());
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(2L)
            .setStatus(ProcessTaskStatus.Running).setProcessInstanceId(1L);
    private ProcessNodeExecutionResultHandler handler;
    private ProcessNodeExecutionLogger logger;

    @BeforeEach
    void setup() {
        when(eventRepository.save(any())).thenAnswer(call -> {
            var event = call.getArgument(0, ProcessInstanceEventEntity.class);
            events.add(event);
            return event;
        });
        when(definition.getOutputs()).thenReturn(List.of());
        when(definition.getPorts()).thenReturn(List.of(new ProcessNodePort("done", "Weiter", "Fortsetzen")));
        when(edges.findByFromNodeIdAndViaPort(3, "done"))
                .thenReturn(Optional.of(new ProcessEdgeEntity(1, 1, 1, 3, 4, "done")));
        var nodes = mock(ProcessNodeRepository.class);
        when(nodes.findById(4)).thenReturn(Optional.empty());
        handler = new ProcessNodeExecutionResultHandler(mock(ProcessAssignmentService.class), mock(RabbitTemplate.class),
                null, instances, tasks, edges, users, null, nodes, mock(ProcessNodeDefinitionService.class), null, null, null);
        logger = new ProcessNodeExecutionLogger(1L, 2L, actor.getId(), null, eventRepository);
    }

    @Test
    void completionCapturesActorRemarkAndAcceptedOutcome() throws Exception {
        var result = ProcessNodeExecutionResultTaskCompleted.of("done")
                .setCompletionHistory(new ProcessNodeCompletionHistory("Freigabe erteilt", "Die Freigabe wurde erteilt.",
                        "Nachweise vollständig.", actor.getId(), null));
        handle(result);

        var event = history().getFirst();
        assertEquals(1, history().size());
        assertFalse(event.getTechnical());
        assertTrue(event.getAudit());
        assertEquals(1L, event.getProcessInstanceId());
        assertEquals(2L, event.getProcessInstanceTaskId());
        assertEquals("actor-id", event.getTriggeringUserId());
        assertEquals("actor-id", event.getConcernedUserId());
        assertTrue(event.getMessage().contains("Ada Beispiel"));
        assertTrue(event.getMessage().contains("Bemerkung: Nachweise vollständig."));
        assertFalse(event.getMessage().contains("actor-id"));
        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
        verify(tasks).save(task);
    }

    @Test
    void rejectedCompletionDoesNotProduceSuccess() {
        var result = ProcessNodeExecutionResultTaskCompleted.of("missing")
                .setCompletionHistory(new ProcessNodeCompletionHistory("Freigabe erteilt", "Freigegeben.", null, actor.getId(), null));
        assertThrows(ProcessNodeExecutionException.class, () -> handle(result));
        assertTrue(history().isEmpty());
        verifyNoInteractions(tasks);
    }

    @Test
    void persistenceFailureDoesNotProduceSuccess() {
        doThrow(new IllegalStateException("unavailable")).when(tasks).save(any());
        var result = ProcessNodeExecutionResultTaskCompleted.of("done")
                .setCompletionHistory(new ProcessNodeCompletionHistory("Freigabe erteilt", "Freigegeben.", null, actor.getId(), null));
        assertThrows(IllegalStateException.class, () -> handle(result));
        assertTrue(history().isEmpty());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "Antragstellende Person"})
    void newIdentityUsesSlotTitleInsteadOfSessionOrIdentifier(String title) throws Exception {
        var identity = new IdentityData("secret-session", "slot-id", IdentityType.Email,
                null, null, null, "mail@example.test", Map.of(), null, Map.of(), title);
        var result = ProcessNodeExecutionResultTaskCompleted.of("done")
                .setCompletionHistory(new ProcessNodeCompletionHistory("Formulardaten eingereicht",
                        "Die angeforderten Daten wurden eingereicht.", null, null, identity.identityId()));
        var customerLogger = new ProcessNodeExecutionLogger(1L, 2L, null, "secret-session", eventRepository);
        handler.handleResultWithAdditionalIdentities(customerLogger, null, definition, node, instance, task, null,
                result, Map.of(identity.identityId(), identity));

        var event = history().getFirst();
        var expected = title == null || title.isBlank() ? "Identität ohne Titel" : title;
        assertEquals("slot-id", event.getConcernedIdentityId());
        assertEquals(expected, event.getConcernedIdentityTitle());
        assertTrue(event.getMessage().contains(expected));
        assertFalse(event.getMessage().contains("slot-id"));
        assertFalse(event.getMessage().contains("secret-session"));
        assertFalse(event.getMessage().contains("mail@example.test"));
        assertEquals(identity, instance.getIdentities().get("slot-id"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void assignmentRemovalUsesFullNameOrNeutralFallback(boolean userExists) throws Exception {
        task.setAssignedUserId("previous-id");
        when(users.retrieve("previous-id")).thenReturn(userExists
                ? Optional.of(new UserEntity().setId("previous-id").setFullName("Erika Beispiel")) : Optional.empty());
        var result = new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(true);
        handle(result);
        handle(result);

        assertEquals(1, history().size());
        var event = history().getFirst();
        assertEquals("previous-id", event.getConcernedUserId());
        assertTrue(event.getMessage().contains(userExists ? "Erika Beispiel" : "Person ohne verfügbaren Namen"));
        assertFalse(event.getMessage().contains("previous-id"));
        assertNull(task.getAssignedUserId());
    }

    @Test
    void technicalCompletionAndAutosaveStayOutOfHistory() throws Exception {
        handle(new ProcessNodeExecutionResultTaskUpdated());
        handle(ProcessNodeExecutionResultTaskCompleted.of("done"));
        assertTrue(history().isEmpty());
        assertFalse(events.isEmpty());
    }

    @Test
    void paymentPollingDoesNotProduceHistory() throws Exception {
        task.setStatus(ProcessTaskStatus.AwaitingPayment);
        handle(new ProcessNodeExecutionResultNoop());
        handle(new ProcessNodeExecutionResultNoop());
        assertTrue(history().isEmpty());
    }

    @Test
    void finishedInstanceIgnoresLateOutcome() throws Exception {
        instance.setStatus(ProcessInstanceStatus.Completed);
        handle(ProcessNodeExecutionResultTaskCompleted.of("done")
                .setCompletionHistory(new ProcessNodeCompletionHistory("Zahlung bestätigt", "Bezahlt.", null, null, null)));
        assertTrue(history().isEmpty());
        verifyNoInteractions(tasks);
    }

    private void handle(ProcessNodeExecutionResult result) throws Exception {
        handler.handleResult(logger, actor, definition, node, instance, task, null, result);
    }

    private List<ProcessInstanceEventEntity> history() {
        return events.stream().filter(ProcessInstanceEventEntity::getHistoryRelevant).toList();
    }
}
