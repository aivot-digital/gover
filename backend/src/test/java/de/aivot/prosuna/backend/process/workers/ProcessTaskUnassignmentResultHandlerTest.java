package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.mail.services.ProcessInstanceMailService;
import de.aivot.prosuna.backend.mail.services.ProcessTaskMailService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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
    private final ProcessAssignmentService assignments = mock(ProcessAssignmentService.class);
    private final CommunicationService communication = mock(CommunicationService.class);
    private final ProcessTaskMailService taskMail = mock(ProcessTaskMailService.class);
    private final ProcessInstanceMailService instanceMail = mock(ProcessInstanceMailService.class);
    private final UserService users = mock(UserService.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ProcessEdgeRepository edges = mock(ProcessEdgeRepository.class);
    private final ProcessNodeDefinition<?> definition = mock(ProcessNodeDefinition.class);
    private final ProcessService processes = mock(ProcessService.class);
    private final DepartmentService departments = mock(DepartmentService.class);
    private final ProcessNodeEntity node = new ProcessNodeEntity().setId(3).setOutputMappings(Map.of());
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L).setProcessId(10)
            .setStatus(ProcessInstanceStatus.Running).setAssignedUserId("instance-owner").setInitialPayload(Map.of());
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(2L)
            .setProcessInstanceId(1L).setAssignedUserId("task-owner").setAssignedCustomerIdentityId("customer");
    private final UserEntity actor = new UserEntity().setId("actor").setFullName("Ada Beispiel");
    private final ProcessNodeExecutionResultHandler handler = new ProcessNodeExecutionResultHandler(
            assignments, rabbit, communication, instances, tasks, edges, users, taskMail,
            mock(ProcessNodeRepository.class), mock(ProcessNodeDefinitionService.class), processes, departments,
            instanceMail);

    @BeforeEach
    void setup() throws Exception {
        when(definition.getOutputs()).thenReturn(List.of());
        when(definition.getPorts()).thenReturn(List.of(new ProcessNodePort("success", "Weiter", "Fortsetzen")));
        when(edges.findByFromNodeIdAndViaPort(3, "success"))
                .thenReturn(Optional.of(new ProcessEdgeEntity(1, 1, 1, 3, 4, "success")));
        when(users.retrieve("task-owner"))
                .thenReturn(Optional.of(new UserEntity().setId("task-owner").setFullName("Anna Beispiel")));
        when(processes.retrieve(10)).thenReturn(Optional.of(new ProcessEntity().setId(10).setDepartmentId(20)));
        when(departments.retrieve(20)).thenReturn(Optional.of(new DepartmentEntity().setId(20).setName("Bürgerservice")));
    }

    @ParameterizedTest
    @MethodSource("resultsSupportingUnassignment")
    void clearsTaskAssignmentAcrossResultTypes(ProcessNodeExecutionResult result, boolean triggeredByUser) throws Exception {
        var user = triggeredByUser ? actor : null;
        result.setClearCurrentlyAssignedUser(true);
        handle(result, user);

        assertNull(task.getAssignedUserId());
        assertEquals("instance-owner", instance.getAssignedUserId());
        assertEquals("customer", task.getAssignedCustomerIdentityId());
        assertAutomaticRemoval(user,
                "Die Aufgabenzuweisung an „Anna Beispiel“ wurde im Prozessablauf automatisch aufgehoben.");
        verifyNoInteractions(taskMail, instanceMail, communication);
    }

    private static Stream<Arguments> resultsSupportingUnassignment() {
        return Stream.of(new ProcessNodeExecutionResultNoop(), new ProcessNodeExecutionResultTaskUpdated(),
                new ProcessNodeExecutionResultTaskCompleted("success"), new ProcessNodeExecutionResultInstanceCompleted())
                .flatMap(result -> Stream.of(false, true).map(triggeredByUser -> Arguments.of(result, triggeredByUser)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void automaticRemovalPreservesItsTriggerAndIsIdempotent(boolean triggeredByUser) throws Exception {
        var user = triggeredByUser ? actor : null;
        var result = new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(true);
        handle(result, user);
        handle(result, user);

        assertAutomaticRemoval(user,
                "Die Aufgabenzuweisung an „Anna Beispiel“ wurde im Prozessablauf automatisch aufgehoben.");
        verify(tasks).save(task);
        verifyNoInteractions(taskMail, instanceMail);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void absentOrFalseFlagPreservesAssignmentWithoutAdditionalWrites(Boolean flag) throws Exception {
        handle(new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(flag), actor);
        assertEquals("task-owner", task.getAssignedUserId());
        verifyNoInteractions(tasks, events, taskMail, instanceMail, users);
    }

    @Test
    void alreadyUnassignedTaskDoesNotSaveOrLog() throws Exception {
        task.setAssignedUserId(null);
        handle(new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(true), actor);
        verifyNoInteractions(tasks, events, taskMail, instanceMail, users);
    }

    @ParameterizedTest
    @MethodSource("identityTitlesAndTriggers")
    void customerAssignmentExplainsTheIdentityHandover(@Nullable String title, String expectedLabel,
                                                      boolean triggeredByUser) throws Exception {
        var user = triggeredByUser ? actor : null;
        var identity = new IdentityData("session", "customer", IdentityType.Email, null, null, null,
                "person@example.test", Map.of(), null, Map.of(), title);
        var identities = new IdentityDataMap();
        identities.put("customer", identity);
        instance.setIdentities(identities);
        handle(ProcessNodeExecutionResultTaskAssignedCustomer.of("customer").setClearCurrentlyAssignedUser(true), user);
        assertNull(task.getAssignedUserId());
        assertEquals("customer", task.getAssignedCustomerIdentityId());
        assertEquals(ProcessTaskStatus.AwaitingCustomer, task.getStatus());
        assertAutomaticRemoval(user,
                "Die Aufgabenzuweisung an „Anna Beispiel“ wurde automatisch aufgehoben, da die Aufgabe an die Identität „"
                        + expectedLabel + "“ übergeben wurde.");
    }

    private static Stream<Arguments> identityTitlesAndTriggers() {
        return Stream.of(false, true).flatMap(triggeredByUser -> Stream.of(
                Arguments.of("Antragstellende Person", "Antragstellende Person", triggeredByUser),
                Arguments.of("  Antragstellende Person  ", "Antragstellende Person", triggeredByUser),
                Arguments.of(null, "customer", triggeredByUser),
                Arguments.of("", "customer", triggeredByUser),
                Arguments.of("  ", "customer", triggeredByUser)
        ));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void emailInvitationExplainsTheHandoverWithoutAnIdentity(boolean triggeredByUser) throws Exception {
        var user = triggeredByUser ? actor : null;
        handle(emailInvitation(), user);

        assertNull(task.getAssignedUserId());
        assertNull(task.getAssignedCustomerIdentityId());
        assertEquals(ProcessTaskStatus.AwaitingCustomer, task.getStatus());
        verify(communication).sendMessageToEmail(eq("person@example.test"), any());
        assertAutomaticRemoval(user,
                "Die Aufgabenzuweisung an „Anna Beispiel“ wurde automatisch aufgehoben, da zur weiteren Bearbeitung per E-Mail eingeladen wurde.");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void paymentRequestExplainsAutomaticRemoval(boolean triggeredByUser) throws Exception {
        var user = triggeredByUser ? actor : null;
        handle(new ProcessNodeExecutionResultPaymentRequested("transaction", "payment-provider")
                .setClearCurrentlyAssignedUser(true), user);

        assertNull(task.getAssignedUserId());
        assertEquals(ProcessTaskStatus.AwaitingPayment, task.getStatus());
        assertAutomaticRemoval(user,
                "Die Aufgabenzuweisung an „Anna Beispiel“ wurde automatisch aufgehoben, da eine Zahlung angefordert wurde.");
    }

    @Test
    void instanceAssignmentCanClearTaskWithoutChangingTheInstanceAssignee() throws Exception {
        var owner = new UserEntity().setId("instance-owner").setFullName("Vorgangsverantwortliche Person");
        when(users.retrieve("instance-owner")).thenReturn(Optional.of(owner));
        handle(ProcessNodeExecutionResultInstanceAssigned.assign("instance-owner")
                .setClearCurrentlyAssignedUser(true), actor);
        assertNull(task.getAssignedUserId());
        assertEquals("instance-owner", instance.getAssignedUserId());
        assertAutomaticRemoval(actor,
                "Die Aufgabenzuweisung an „Anna Beispiel“ wurde im Prozessablauf automatisch aufgehoben.");
    }

    @Test
    void staffUpdateStillRecordsItsActualActorSeparatelyFromAutomaticRemoval() throws Exception {
        handle(new ProcessNodeExecutionResultTaskUpdated().setClearCurrentlyAssignedUser(true), actor);
        assertNull(task.getAssignedUserId());
        verify(events).save(argThat(event -> event.getLevel() == ProcessNodeExecutionLogLevel.Debug
                && "actor".equals(event.getTriggeringUserId()) && event.getMessage().contains("Ada Beispiel")));
        assertAutomaticRemoval(actor,
                "Die Aufgabenzuweisung an „Anna Beispiel“ wurde im Prozessablauf automatisch aufgehoben.");
    }

    @Test
    void conflictingStaffAssignmentIsRejectedBeforeCommunicationOrPersistence() {
        var result = ProcessNodeExecutionResultTaskAssigned.of("new-owner")
                .setClearCurrentlyAssignedUser(true)
                .setCommunicationRequest(ProcessNodeExecutionResultCommunicationRequest.toEmail(
                        "person@example.test", CommunicationMessage.of("Test", "Testnachricht", "<p>Testnachricht</p>")));
        assertThrows(ProcessNodeExecutionExceptionBrokenImplementation.class, () -> handle(result, actor));
        assertEquals("task-owner", task.getAssignedUserId());
        verifyNoInteractions(tasks, instances, events, assignments, communication, taskMail, instanceMail, users, rabbit);
    }

    @Test
    void failedResultKeepsAssignmentAndDoesNotLogRemoval() {
        assertThrows(ProcessNodeExecutionExceptionBrokenImplementation.class, () -> handle(
                new ProcessNodeExecutionResultTaskCompleted("missing-port").setClearCurrentlyAssignedUser(true), actor));
        assertEquals("task-owner", task.getAssignedUserId());
        verifyNoInteractions(tasks, events);
    }

    @Test
    void failedRemovalSaveDoesNotLogSuccess() {
        doThrow(new IllegalStateException("database unavailable")).when(tasks).save(task);
        assertThrows(IllegalStateException.class, () -> handle(
                new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(true), actor));
        verifyNoInteractions(events);
    }

    @Test
    void failedInvitationPreservesAssignmentAndDoesNotLogRemoval() throws Exception {
        doThrow(mock(CommunicationException.class)).when(communication).sendMessageToEmail(anyString(), any());

        assertThrows(ProcessNodeExecutionException.class, () -> handle(emailInvitation(), actor));

        assertEquals("task-owner", task.getAssignedUserId());
        assertEquals(ProcessTaskStatus.Failed, task.getStatus());
        verifyNoInteractions(events);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = {"Completed", "Aborted"})
    void terminalInstanceDoesNotProcessRemoval(ProcessInstanceStatus status) throws Exception {
        instance.setStatus(status);
        handle(new ProcessNodeExecutionResultNoop().setClearCurrentlyAssignedUser(true), actor);
        assertEquals("task-owner", task.getAssignedUserId());
        verifyNoInteractions(tasks, events);
    }

    private ProcessNodeExecutionResult emailInvitation() {
        return ProcessNodeExecutionResultTaskAssignedCustomer.withoutIdentity()
                .setClearCurrentlyAssignedUser(true)
                .setCommunicationRequest(ProcessNodeExecutionResultCommunicationRequest.toEmail(
                        "person@example.test", CommunicationMessage.of("Test", "Testnachricht", "<p>Testnachricht</p>")));
    }

    private void handle(ProcessNodeExecutionResult result, @Nullable UserEntity user) throws ProcessNodeExecutionException {
        var logger = new ProcessNodeExecutionLogger(1L, 2L, user == null ? null : user.getId(), null, events);
        handler.handleResult(logger, user, definition, node, instance, task, null, result);
    }

    private void assertAutomaticRemoval(@Nullable UserEntity user, String message) {
        var captor = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(events, atLeastOnce()).save(captor.capture());
        var removals = captor.getAllValues().stream()
                .filter(event -> "Aufgabenzuweisung automatisch aufgehoben".equals(event.getTitle())).toList();
        assertEquals(1, removals.size());
        var event = removals.getFirst();
        assertEquals(ProcessNodeExecutionLogLevel.Info, event.getLevel());
        assertFalse(event.getTechnical());
        assertTrue(event.getAudit());
        assertTrue(event.getHistoryRelevant());
        assertEquals(1L, event.getProcessInstanceId());
        assertEquals(2L, event.getProcessInstanceTaskId());
        assertEquals(user == null ? null : user.getId(), event.getTriggeringUserId());
        assertEquals("task-owner", event.getConcernedUserId());
        assertEquals(Map.of("previousAssignedUserId", "task-owner"), event.getDetails());
        assertEquals(message, event.getMessage());
        assertEquals("instance-owner", instance.getAssignedUserId());
    }
}
