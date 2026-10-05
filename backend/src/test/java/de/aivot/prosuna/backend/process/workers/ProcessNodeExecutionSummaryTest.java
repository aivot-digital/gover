package de.aivot.prosuna.backend.process.workers;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.services.ProcessInstanceMailService;
import de.aivot.prosuna.backend.mail.services.ProcessTaskMailService;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionBrokenImplementation;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessNodeExecutionSummaryTest {
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessVersionRepository versions = mock(ProcessVersionRepository.class);
    private final ProcessEdgeRepository edges = mock(ProcessEdgeRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final UserService users = mock(UserService.class);
    private final ProcessNodeExecutionLogger executionLogger = mock(ProcessNodeExecutionLogger.class);
    @SuppressWarnings("unchecked")
    private final ProcessNodeDefinition<Configuration> definition = mock(ProcessNodeDefinition.class, CALLS_REAL_METHODS);
    private final Configuration configuration = new Configuration("Zählerstand");
    private final ProcessNodeEntity node = new ProcessNodeEntity().setId(3).setName("Zähler")
            .setProcessId(1).setProcessVersion(1)
            .setProcessNodeDefinitionKey("test/counter").setOutputMappings(Map.of("value", "total"));
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L)
            .setStatus(ProcessInstanceStatus.Running).setInitialPayload(Map.of("original", true))
            .setIdentities(new IdentityDataMap());
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(2L)
            .setProcessInstanceId(1L).setStatus(ProcessTaskStatus.Running);
    private final UserEntity actor = new UserEntity().setId("actor").setFullName("Ada Beispiel");
    private final ProcessNodeExecutionResultHandler handler = new ProcessNodeExecutionResultHandler(
            mock(ProcessAssignmentService.class), rabbit, null, instances, versions, tasks, edges, users,
            mock(ProcessTaskMailService.class), mock(ProcessNodeRepository.class),
            mock(ProcessNodeDefinitionService.class), null, null, mock(ProcessInstanceMailService.class));

    @BeforeEach
    void setup() throws Exception {
        when(versions.findById(ProcessVersionEntityId.of(1, 1)))
                .thenReturn(Optional.of(new ProcessVersionEntity().setProcessId(1).setProcessVersion(1)));
        when(definition.getOutputs()).thenReturn(List.of(new ProcessNodeOutput("value", "Wert", "Ergebnis", "number")));
        when(definition.getPorts()).thenReturn(List.of(new ProcessNodePort("next", "Weiter", "Fortsetzen")));
        when(edges.findByFromNodeIdAndViaPort(3, "next"))
                .thenReturn(Optional.of(new ProcessEdgeEntity(1, 1, 1, 3, 4, "next")));
        when(users.retrieve("actor")).thenReturn(Optional.of(actor));
        instance.getIdentities().put("customer", identity("customer"));
    }

    @Test
    void taskCompletionUsesEffectiveConfigurationAndMappedDataBeforeSavingAndDispatching() throws Exception {
        var previousTask = new ProcessInstanceTaskEntity().setProcessData(Map.of("previous", true));
        var identity = identity("new-identity");
        when(definition.generateExecutionSummary(any())).thenAnswer(invocation -> {
            ProcessNodeExecutionSummaryContext<Configuration> context = invocation.getArgument(0);
            assertSame(configuration, context.configurationOfExecutingNode());
            assertSame(node, context.thisNode());
            assertSame(instance, context.thisProcessInstance());
            assertSame(task, context.thisTask());
            assertSame(previousTask, context.previousTask());
            assertSame(actor, context.triggeringUser());
            assertEquals("next", context.viaPort());
            assertEquals(ProcessTaskStatus.Completed, context.thisTask().getStatus());
            assertNotNull(context.thisTask().getFinished());
            assertEquals(Map.of("saved", true), context.thisTask().getRuntimeData());
            assertEquals(Map.of("value", 17), context.thisTask().getNodeData());
            assertEquals(Map.of("previous", true, "total", 17), context.thisTask().getProcessData());
            assertSame(identity, context.thisProcessInstance().getIdentities().get("new-identity"));
            return "**" + context.configurationOfExecutingNode().label() + ":** " + context.thisTask().getProcessData().get("total");
        });
        doAnswer(invocation -> {
            assertEquals("**Zählerstand:** 17", task.getExecutionSummaryMarkdown());
            return task;
        }).when(tasks).save(task);

        handler.handleResultWithAdditionalIdentities(executionLogger, actor, definition, configuration, node, instance,
                task, previousTask, new ProcessNodeExecutionResultTaskCompleted("next")
                        .setNodeData(Map.of("value", 17)).setRuntimeData(Map.of("saved", true)),
                Map.of("new-identity", identity));

        var order = inOrder(definition, tasks, rabbit);
        order.verify(definition).generateExecutionSummary(any());
        order.verify(tasks).save(task);
        order.verify(rabbit).convertAndSend(eq(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE), any(ProcessWorker.DoWorkWorkerPayload.class));
        verify(definition, times(1)).generateExecutionSummary(any());
    }

    @Test
    void instanceCompletionExposesFinalInstanceStateAndHasNoOutgoingPort() throws Exception {
        var retentionDate = Instant.now().plusSeconds(3600);
        when(definition.generateExecutionSummary(any())).thenAnswer(invocation -> {
            ProcessNodeExecutionSummaryContext<Configuration> context = invocation.getArgument(0);
            assertNull(context.viaPort());
            assertNull(context.previousTask());
            assertNull(context.triggeringUser());
            assertEquals(ProcessInstanceStatus.Completed, context.thisProcessInstance().getStatus());
            assertEquals(context.thisTask().getFinished(), context.thisProcessInstance().getFinished());
            assertEquals(retentionDate, context.thisProcessInstance().getKeepUntil());
            assertEquals(17, context.thisTask().getProcessData().get("total"));
            return "Vorgang abgeschlossen.";
        });

        handle(new ProcessNodeExecutionResultInstanceCompleted().setRetentionDate(retentionDate).setNodeData(Map.of("value", 17)));

        assertEquals("Vorgang abgeschlossen.", task.getExecutionSummaryMarkdown());
        verify(tasks).save(task);
        verify(instances).save(instance);
        verifyNoInteractions(rabbit);
    }

    @Test
    void instanceAssignmentWithContinuationAlsoGeneratesSummary() throws Exception {
        when(definition.generateExecutionSummary(any())).thenReturn("Zuweisung abgeschlossen.");

        handle(ProcessNodeExecutionResultInstanceAssigned.assignAndContinue("actor", "next").setNodeData(Map.of("value", 17)));

        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
        assertEquals("Zuweisung abgeschlossen.", task.getExecutionSummaryMarkdown());
        verify(definition).generateExecutionSummary(argThat(context -> context.configurationOfExecutingNode() == configuration
                && "next".equals(context.viaPort()) && "actor".equals(context.thisProcessInstance().getAssignedUserId())));
        verify(tasks).save(task);
    }

    @Test
    void completionPreservesEarlierNotificationReceiptWhenNodeClearsRuntimeData() throws Exception {
        var receipt = Map.<String, Object>of("sentAt", "2026-10-01T07:00:00Z", "deliveryChannel", "E-Mail");
        task.setRuntimeData(Map.of("executionSummary", receipt, "draft", true));
        when(definition.generateExecutionSummary(any())).thenAnswer(invocation -> {
            ProcessNodeExecutionSummaryContext<Configuration> context = invocation.getArgument(0);
            assertEquals(Map.of("executionSummary", receipt), context.thisTask().getRuntimeData());
            return "Eingereicht.";
        });
        handle(new ProcessNodeExecutionResultTaskCompleted("next").setRuntimeData(Map.of()));
        assertEquals("Eingereicht.", task.getExecutionSummaryMarkdown());
    }

    @Test
    void unassignmentCapturesPreviousUserBeforeClearingInstance() throws Exception {
        instance.setAssignedUserId("actor");
        when(definition.generateExecutionSummary(any())).thenAnswer(invocation -> {
            ProcessNodeExecutionSummaryContext<Configuration> context = invocation.getArgument(0);
            assertNull(context.thisProcessInstance().getAssignedUserId());
            var snapshot = (Map<?, ?>) context.thisTask().getRuntimeData().get("executionSummary");
            assertEquals("actor", snapshot.get("previousAssignedUserId"));
            assertEquals("Ada Beispiel", snapshot.get("previousAssignedUserName"));
            return "Zuweisung entfernt.";
        });
        handle(ProcessNodeExecutionResultInstanceAssigned.clear().setViaPort("next"));
        assertEquals("Zuweisung entfernt.", task.getExecutionSummaryMarkdown());
    }

    @Test
    void defaultImplementationCompletesWithoutSummary() throws Exception {
        handle(new ProcessNodeExecutionResultTaskCompleted("next"));
        assertNull(task.getExecutionSummaryMarkdown());
        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
        verify(tasks).save(task);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\n\t"})
    void absentOrBlankSummaryIsStoredAsNull(String summary) throws Exception {
        when(definition.generateExecutionSummary(any())).thenReturn(summary);
        handle(new ProcessNodeExecutionResultTaskCompleted("next"));
        assertNull(task.getExecutionSummaryMarkdown());
        assertEquals(ProcessTaskStatus.Completed, task.getStatus());
    }

    @Test
    void preservesMarkdownWhitespace() throws Exception {
        var summary = "    code block\n\nText with a line break  \nNext line  ";
        when(definition.generateExecutionSummary(any())).thenReturn(summary);
        handle(new ProcessNodeExecutionResultTaskCompleted("next"));
        assertEquals(summary, task.getExecutionSummaryMarkdown());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void summaryFailureIsLoggedWithoutFailingOrRetryingCompletion(boolean completeInstance) throws Exception {
        var error = ResponseException.internalServerError("Summary failed");
        when(definition.generateExecutionSummary(any())).thenThrow(error);
        var logger = (Logger) LoggerFactory.getLogger(ProcessNodeExecutionResultHandler.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            handle(completeInstance ? new ProcessNodeExecutionResultInstanceCompleted() : new ProcessNodeExecutionResultTaskCompleted("next"));
            assertEquals(ProcessTaskStatus.Completed, task.getStatus());
            assertNull(task.getExecutionSummaryMarkdown());
            verify(tasks).save(task);
            assertTrue(appender.list.stream().anyMatch(event -> event.getFormattedMessage().contains("task 2 (node definition test/counter)")
                    && event.getThrowableProxy() != null));
            if (completeInstance) {
                assertEquals(ProcessInstanceStatus.Completed, instance.getStatus());
                verifyNoInteractions(rabbit);
            } else {
                verify(rabbit).convertAndSend(eq(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE), any(ProcessWorker.DoWorkWorkerPayload.class));
            }
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @ParameterizedTest
    @MethodSource("intermediateResults")
    void intermediateResultsDoNotGenerateSummary(ProcessNodeExecutionResult result) throws Exception {
        handle(result);
        verify(definition, never()).generateExecutionSummary(any());
        assertNull(task.getExecutionSummaryMarkdown());
    }

    private static Stream<ProcessNodeExecutionResult> intermediateResults() {
        return Stream.of(new ProcessNodeExecutionResultTaskUpdated(), new ProcessNodeExecutionResultNoop(),
                new ProcessNodeExecutionResultPaymentRequested("transaction", "provider"),
                ProcessNodeExecutionResultTaskAssigned.of("actor"), ProcessNodeExecutionResultInstanceAssigned.assign("actor"),
                ProcessNodeExecutionResultTaskAssignedCustomer.of("customer"));
    }

    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = {"Completed", "Aborted"})
    void terminalInstancesIgnoreLateCompletion(ProcessInstanceStatus status) throws Exception {
        instance.setStatus(status);
        handle(new ProcessNodeExecutionResultTaskCompleted("next"));
        verify(definition, never()).generateExecutionSummary(any());
        verifyNoInteractions(tasks, rabbit);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void repeatedCompletionPreservesStoredSummaryEvenWhenAbsent(boolean hasSummary) throws Exception {
        when(definition.generateExecutionSummary(any())).thenReturn(hasSummary ? "Original" : null);
        handle(new ProcessNodeExecutionResultTaskCompleted("next"));
        when(definition.generateExecutionSummary(any())).thenReturn("Changed later");
        handle(new ProcessNodeExecutionResultTaskCompleted("next"));
        assertEquals(hasSummary ? "Original" : null, task.getExecutionSummaryMarkdown());
        verify(definition, times(1)).generateExecutionSummary(any());
        verify(tasks, times(1)).save(task);
        verify(rabbit, times(1)).convertAndSend(eq(ProcessWorker.DO_WORK_ON_INSTANCE_QUEUE), any(ProcessWorker.DoWorkWorkerPayload.class));
    }

    @Test
    void invalidCompletionPathDoesNotGenerateSummary() throws Exception {
        assertThrows(ProcessNodeExecutionExceptionBrokenImplementation.class,
                () -> handle(new ProcessNodeExecutionResultTaskCompleted("missing")));
        verify(definition, never()).generateExecutionSummary(any());
        verifyNoInteractions(tasks, rabbit);
    }

    @Test
    void persistenceFailureIsNotSwallowedAsSummaryFailure() throws Exception {
        when(definition.generateExecutionSummary(any())).thenReturn("Completed");
        var failure = new IllegalStateException("Database unavailable");
        when(tasks.save(task)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> handle(new ProcessNodeExecutionResultTaskCompleted("next"))));
        verifyNoInteractions(rabbit);
    }

    private void handle(ProcessNodeExecutionResult result) throws Exception {
        handler.handleResult(executionLogger, null, definition, configuration, node, instance, task, null, result);
    }

    private static IdentityData identity(String id) {
        return new IdentityData("session", id, IdentityType.Email, null, null, null,
                "person@example.test", Map.of(), null, Map.of());
    }

    private record Configuration(String label) {
    }
}
