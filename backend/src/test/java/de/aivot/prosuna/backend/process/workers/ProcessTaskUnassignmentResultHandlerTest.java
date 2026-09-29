package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.mail.services.ProcessInstanceMailService;
import de.aivot.prosuna.backend.mail.services.ProcessTaskMailService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessParticipationService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessTaskUnassignmentResultHandlerTest {
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceHistoryEventRepository events = mock(ProcessInstanceHistoryEventRepository.class);
    private final ProcessParticipationService participations = mock(ProcessParticipationService.class);
    private final ProcessAssignmentService assignments = mock(ProcessAssignmentService.class);
    private final CommunicationService communication = mock(CommunicationService.class);
    private final ProcessTaskMailService taskMail = mock(ProcessTaskMailService.class);
    private final ProcessInstanceMailService instanceMail = mock(ProcessInstanceMailService.class);
    private final UserService users = mock(UserService.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ProcessEdgeRepository edges = mock(ProcessEdgeRepository.class);
    private final ProcessNodeDefinition<?> definition = mock(ProcessNodeDefinition.class);
    private final RecordingTransactions transactions = new RecordingTransactions();
    private final ProcessNodeEntity node = new ProcessNodeEntity().setId(3).setOutputMappings(Map.of());
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L)
            .setStatus(ProcessInstanceStatus.Running).setAssignedUserId("instance-owner").setInitialPayload(Map.of());
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(2L)
            .setProcessInstanceId(1L).setAssignedUserId("task-owner").setAssignedCustomerIdentityId("customer");
    private final UserEntity actor = new UserEntity().setId("actor").setFullName("Ada Beispiel");
    private final ProcessNodeExecutionResultHandler handler = new ProcessNodeExecutionResultHandler(
            assignments, rabbit, communication, instances, tasks, edges, users, taskMail,
            mock(ProcessNodeRepository.class), mock(ProcessNodeDefinitionService.class), null, null,
            instanceMail, participations, transactions);

    @BeforeEach
    void setup() {
        when(definition.getOutputs()).thenReturn(List.of());
        when(definition.getPorts()).thenReturn(List.of(new ProcessNodePort("success", "Weiter", "Fortsetzen")));
        when(edges.findByFromNodeIdAndViaPort(3, "success"))
                .thenReturn(Optional.of(new ProcessEdgeEntity(1, 1, 1, 3, 4, "success")));
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return call.getArgument(0);
        }).when(tasks).save(any());
        doAnswer(call -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            return call.getArgument(0);
        }).when(events).save(any());
    }

    @ParameterizedTest
    @MethodSource("resultsSupportingUnassignment")
    void clearsTaskAssignmentAcrossResultTypes(ProcessNodeExecutionResult result) throws Exception {
        result.setClearCurrentlyAssignedUser(true);
        handle(result, null);

        assertNull(task.getAssignedUserId());
        assertEquals("instance-owner", instance.getAssignedUserId());
        assertEquals("customer", task.getAssignedCustomerIdentityId());
        var event = removalEvent();
        assertEquals(ProcessNodeExecutionLogLevel.Info, event.getLevel());
        assertTrue(event.getTechnical());
        assertTrue(event.getAudit());
        assertEquals(1L, event.getProcessInstanceId());
        assertEquals(2L, event.getProcessInstanceTaskId());
        assertNull(event.getTriggeringUserId());
        assertEquals(Map.of("previousAssignedUserId", "task-owner"), event.getDetails());
        assertTrue(event.getMessage().contains("task-owner"));
        assertTrue(event.getMessage().contains("automatisch"));
        verifyNoInteractions(participations, taskMail, instanceMail, communication, users);
        assertEquals(1, transactions.commits);
    }

    private static Stream<ProcessNodeExecutionResult> resultsSupportingUnassignment() {
        return Stream.of(new ProcessNodeExecutionResultNoop(), new ProcessNodeExecutionResultTaskUpdated(),
                new ProcessNodeExecutionResultTaskCompleted("success"), new ProcessNodeExecutionResultInstanceCompleted(),
                new ProcessNodeExecutionResultPaymentRequested("transaction", "payment-provider"));
    }

    @Test
    void manualNoopRemovalLogsActorWithoutChangingParticipationAndIsIdempotent() throws Exception {
        var result = new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(true);
        handle(result, actor);
        handle(result, actor);

        var event = removalEvent();
        assertFalse(event.getTechnical());
        assertEquals("actor", event.getTriggeringUserId());
        assertTrue(event.getMessage().contains("Ausgelöst durch „Ada Beispiel“"));
        verify(tasks).save(task);
        verifyNoInteractions(participations, taskMail, instanceMail);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void absentOrFalseFlagPreservesAssignmentWithoutAdditionalWrites(Boolean flag) throws Exception {
        handle(new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(flag), actor);
        assertEquals("task-owner", task.getAssignedUserId());
        verifyNoInteractions(tasks, events, participations, taskMail, instanceMail);
    }

    @Test
    void alreadyUnassignedTaskDoesNotSaveOrLog() throws Exception {
        task.setAssignedUserId(null);
        handle(new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(true), actor);
        verifyNoInteractions(tasks, events, participations, taskMail, instanceMail);
    }

    @Test
    void customerAssignmentStillRecordsOnlyItsIdentity() throws Exception {
        var identity = new IdentityData("session", "customer", IdentityType.Email, null, null, null,
                "person@example.test", Map.of(), null, Map.of(), "Antragstellende Person");
        var identities = new IdentityDataMap();
        identities.put("customer", identity);
        instance.setIdentities(identities);
        handle(ProcessNodeExecutionResultTaskAssignedCustomer.of("customer").setClearCurrentlyAssignedUser(true), actor);
        assertNull(task.getAssignedUserId());
        assertEquals("customer", task.getAssignedCustomerIdentityId());
        verify(participations).recordIdentity(eq(1L), eq(2L), same(identity), any());
        verifyNoMoreInteractions(participations);
        removalEvent();
    }

    @Test
    void instanceAssignmentCanClearTaskWithoutAddingExtraParticipants() throws Exception {
        var owner = new UserEntity().setId("instance-owner").setFullName("Vorgangsverantwortliche Person");
        when(users.retrieve("instance-owner")).thenReturn(Optional.of(owner));
        handle(ProcessNodeExecutionResultInstanceAssigned.assign("instance-owner")
                .setClearCurrentlyAssignedUser(true), actor);
        assertNull(task.getAssignedUserId());
        assertEquals("instance-owner", instance.getAssignedUserId());
        verify(participations).recordUser(eq(1L), eq(2L), same(owner), any());
        verifyNoMoreInteractions(participations);
        removalEvent();
    }

    @Test
    void staffUpdateStillRecordsItsActualActor() throws Exception {
        handle(new ProcessNodeExecutionResultTaskUpdated().setClearCurrentlyAssignedUser(true), actor);
        assertNull(task.getAssignedUserId());
        verify(participations).recordUser(eq(1L), eq(2L), same(actor), any());
        verifyNoMoreInteractions(participations);
    }

    @Test
    void conflictingStaffAssignmentIsRejectedBeforeCommunicationOrPersistence() {
        var result = ProcessNodeExecutionResultTaskAssigned.of("new-owner")
                .setClearCurrentlyAssignedUser(true)
                .setCommunicationRequest(ProcessNodeExecutionResultCommunicationRequest.toEmail(
                        "person@example.test", CommunicationMessage.of("Test", "Testnachricht", "<p>Testnachricht</p>")));
        assertThrows(ProcessNodeExecutionExceptionBrokenImplementation.class, () -> handle(result, actor));
        assertEquals("task-owner", task.getAssignedUserId());
        verifyNoInteractions(tasks, instances, events, participations, assignments, communication, taskMail, instanceMail, users, rabbit);
        assertEquals(0, transactions.commits);
        assertEquals(0, transactions.rollbacks);
    }

    @Test
    void failedResultKeepsAssignmentAndDoesNotLogRemoval() {
        assertThrows(ProcessNodeExecutionExceptionBrokenImplementation.class, () -> handle(
                new ProcessNodeExecutionResultTaskCompleted("missing-port").setClearCurrentlyAssignedUser(true), actor));
        assertEquals("task-owner", task.getAssignedUserId());
        verifyNoInteractions(tasks, events, participations);
        assertEquals(1, transactions.rollbacks);
    }

    @Test
    void failedRemovalSaveRollsBackWithoutSuccessLog() {
        doThrow(new IllegalStateException("database unavailable")).when(tasks).save(task);
        assertThrows(IllegalStateException.class, () -> handle(
                new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(true), actor));
        verifyNoInteractions(events, participations);
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
    }

    @Test
    void failureAfterRemovalRollsBackTheTransactionContainingItsLog() {
        doThrow(new IllegalStateException("participation unavailable"))
                .when(participations).recordUser(any(), any(), any(), any());
        assertThrows(IllegalStateException.class, () -> handle(
                new ProcessNodeExecutionResultTaskUpdated().setClearCurrentlyAssignedUser(true), actor));
        // Both saves were made inside the same transaction, which must roll back as a whole.
        removalEvent();
        assertEquals(1, transactions.rollbacks);
        assertEquals(0, transactions.commits);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = {"Completed", "Aborted"})
    void terminalInstanceDoesNotProcessRemoval(ProcessInstanceStatus status) throws Exception {
        instance.setStatus(status);
        handle(new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(true), actor);
        assertEquals("task-owner", task.getAssignedUserId());
        verifyNoInteractions(tasks, events, participations);
    }

    private void handle(ProcessNodeExecutionResult result, @Nullable UserEntity user) throws ProcessNodeExecutionException {
        var logger = new ProcessNodeExecutionLogger(1L, 2L, user == null ? null : user.getId(), null, events);
        handler.handleResult(logger, user, definition, node, instance, task, null, result);
    }

    private ProcessInstanceEventEntity removalEvent() {
        var captor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(events, atLeastOnce()).save(captor.capture());
        var removals = captor.getAllValues().stream()
                .filter(event -> "Aufgabenzuweisung aufgehoben".equals(event.getTitle())).toList();
        assertEquals(1, removals.size());
        return removals.getFirst();
    }

    private static class RecordingTransactions extends AbstractPlatformTransactionManager {
        int commits;
        int rollbacks;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
