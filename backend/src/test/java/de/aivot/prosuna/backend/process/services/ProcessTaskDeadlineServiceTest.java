package de.aivot.prosuna.backend.process.services;

import de.aivot.prosuna.backend.core.services.BusinessTime;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.workers.ProcessTaskDeadlineResultHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcessTaskDeadlineServiceTest {
    private static final Instant DEADLINE = Instant.parse("2030-05-01T12:00:00Z");
    private final ProcessNodeRepository nodes = mock(ProcessNodeRepository.class);
    private final ProcessNodeService nodeService = mock(ProcessNodeService.class);
    private final ProcessEdgeRepository edges = mock(ProcessEdgeRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessTaskDeadlineContinuationRepository continuations = mock(ProcessTaskDeadlineContinuationRepository.class);
    private final ProcessInstanceHistoryEventRepository events = mock(ProcessInstanceHistoryEventRepository.class);
    private final ProcessNodeExecutionLoggerFactory loggers = mock(ProcessNodeExecutionLoggerFactory.class);
    private final Clock clock = mock(Clock.class);
    private final BusinessTime time = new BusinessTime(ZoneOffset.UTC, clock);
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(1L).setStatus(ProcessInstanceStatus.Running);
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setId(2L).setProcessInstanceId(1L)
            .setProcessNodeId(3).setStatus(ProcessTaskStatus.AwaitingCustomer).setDeadline(DEADLINE).setDeadlineKey("customer")
            .setDeadlineStrict(true).setNodeData(Map.of("existing", "value")).setProcessData(Map.of("keep", true))
            .setRuntimeData(Map.of("phase", "customer")).setAssignedUserId("staff").setAssignedCustomerIdentityId("customer-id");
    private final ProcessNodeEntity node = new ProcessNodeEntity().setId(3)
            .setDeadlines(Map.of("customer", new ProcessNodeDeadlineConfiguration(2, true)));
    private final ProcessNodeDeadlineDefinition definition = new ProcessNodeDeadlineDefinition("customer", "Antwort der Kund:in",
            "Zeit für die Antwort", new ProcessNodePort("customer_overdue", "Antwort überfällig", "Fortsetzung"));
    private final ProcessTaskDeadlineService service = new ProcessTaskDeadlineService(nodes, nodeService, edges, instances,
            tasks, loggers, new ProcessTaskDeadlineResultHandler(tasks, continuations), time);

    @BeforeEach
    void setup() {
        when(clock.instant()).thenReturn(DEADLINE);
        when(nodes.findById(3)).thenReturn(Optional.of(node));
        when(nodes.findById(4)).thenReturn(Optional.of(new ProcessNodeEntity().setId(4)));
        when(nodeService.resolvePorts(node)).thenReturn(new ProcessNodePortResolution(List.of(new ProcessNodePort(
                "customer_overdue", "Antwort überfällig", "Fortsetzung", "customer")), List.of(), List.of(definition)));
        when(edges.findByFromNodeIdAndViaPort(3, "customer_overdue")).thenReturn(Optional.of(new ProcessEdgeEntity()
                .setFromNodeId(3).setToNodeId(4).setViaPort("customer_overdue")));
        when(loggers.create(1L, 2L, null, null)).thenReturn(new ProcessNodeExecutionLogger(1L, 2L, null, null, events));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 3600})
    void expiresAtAndAfterDeadlineExactlyOnce(long delay) {
        var now = DEADLINE.plusSeconds(delay);
        when(clock.instant()).thenReturn(now);
        task.setPostponedUntil(DEADLINE.plusSeconds(86400));

        service.expireIfDue(instance, task);
        service.expireIfDue(instance, task);

        assertEquals(ProcessTaskStatus.DeadlineExceeded, task.getStatus());
        assertTrue(task.getStatus().isTerminal());
        assertNotEquals(ProcessTaskStatus.Completed, task.getStatus());
        assertEquals(now, task.getFinished());
        assertEquals(DEADLINE, task.getDeadline());
        assertEquals("customer", task.getDeadlineKey());
        assertEquals("value", task.getNodeData().get("existing"));
        assertEquals(Map.of("deadlineKey", "customer", "label", "Antwort der Kund:in", "deadline", DEADLINE.toString(),
                "processedAt", now.toString()), task.getNodeData().get(ProcessTaskDeadlineExceededData.NODE_DATA_KEY));
        assertEquals(Map.of("keep", true), task.getProcessData());
        assertEquals(Map.of("phase", "customer"), task.getRuntimeData());
        assertEquals("staff", task.getAssignedUserId());
        assertEquals("customer-id", task.getAssignedCustomerIdentityId());
        assertNull(task.getStatusOverride());
        assertTrue(task.getExecutionSummaryMarkdown().contains("Antwort der Kund:in"));
        assertEquals(ProcessInstanceStatus.Running, instance.getStatus());

        var continuation = ArgumentCaptor.forClass(ProcessTaskDeadlineContinuationEntity.class);
        verify(continuations).save(continuation.capture());
        assertEquals(2L, continuation.getValue().getTaskId());
        assertEquals(4, continuation.getValue().getToNodeId());
        assertEquals("customer_overdue", continuation.getValue().getViaPort());
        var event = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(events).save(event.capture());
        assertTrue(event.getValue().getHistoryRelevant());
        assertTrue(event.getValue().getAudit());
        assertEquals("Frist überschritten", event.getValue().getTitle());
    }

    @Test
    void doesNotExpireBeforeDeadline() {
        when(clock.instant()).thenReturn(DEADLINE.minusNanos(1));
        service.expireIfDue(instance, task);
        verifyNoInteractions(nodes, continuations, events);
    }

    @Test
    void softDeadlineHasNoConsequences() {
        task.setDeadlineStrict(false);
        service.expireIfDue(instance, task);
        assertEquals(ProcessTaskStatus.AwaitingCustomer, task.getStatus());
        verifyNoInteractions(nodes, nodeService, tasks, continuations, events);
    }

    @Test
    void deadlineNoLongerHardOnTheElementBecomesSoftWithoutConsequences() {
        node.setDeadlines(Map.of("customer", new ProcessNodeDeadlineConfiguration(2, false)));

        service.expireIfDue(instance, task);
        service.expireIfDue(instance, task);

        assertEquals(ProcessTaskStatus.AwaitingCustomer, task.getStatus());
        assertFalse(task.getDeadlineStrict());
        assertEquals(DEADLINE, task.getDeadline());
        assertEquals("customer", task.getDeadlineKey());
        verify(tasks).save(task);
        verify(nodes).findById(3);
        verifyNoInteractions(nodeService, continuations, events);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = {"Paused", "Completed", "Aborted", "Failed", "Restarted", "DeadlineExceeded"})
    void skipsPausedAndTerminalTasks(ProcessTaskStatus status) {
        task.setStatus(status);
        service.expireIfDue(instance, task);
        assertEquals(status, task.getStatus());
        verifyNoInteractions(nodes, continuations, events);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessInstanceStatus.class, names = "Running", mode = EnumSource.Mode.EXCLUDE)
    void skipsInstancesThatAreNotRunning(ProcessInstanceStatus status) {
        instance.setStatus(status);
        service.expireIfDue(instance, task);
        assertEquals(DEADLINE, task.getDeadline());
        verifyNoInteractions(nodes, continuations, events);
    }

    @Test
    void resumedTaskUsesUnchangedDeadlineAndCurrentPhase() {
        task.setStatus(ProcessTaskStatus.Paused);
        service.expireIfDue(instance, task);
        task.setStatus(ProcessTaskStatus.AwaitingCustomer);
        service.expireIfDue(instance, task);
        verify(edges).findByFromNodeIdAndViaPort(3, "customer_overdue");
        assertEquals(ProcessTaskStatus.DeadlineExceeded, task.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"definition", "port", "edge", "target", "configuration"})
    void invalidDeadlinePathFailsTechnicallyWithoutExpiryContinuation(String missing) {
        switch (missing) {
            case "definition" -> when(nodeService.resolvePorts(node)).thenReturn(new ProcessNodePortResolution(List.of(), List.of()));
            case "port" -> when(nodeService.resolvePorts(node)).thenReturn(new ProcessNodePortResolution(List.of(), List.of(), List.of(definition)));
            case "edge" -> when(edges.findByFromNodeIdAndViaPort(3, "customer_overdue")).thenReturn(Optional.empty());
            case "target" -> when(nodes.findById(4)).thenReturn(Optional.empty());
            case "configuration" -> when(nodeService.resolvePorts(node)).thenReturn(new ProcessNodePortResolution(List.of(), List.of("Ungültige Frist")));
        }
        service.expireIfDue(instance, task);
        assertEquals(ProcessTaskStatus.Failed, task.getStatus());
        assertEquals(ProcessInstanceStatus.Failed, instance.getStatus());
        assertFalse(task.getNodeData().containsKey(ProcessTaskDeadlineExceededData.NODE_DATA_KEY));
        verifyNoInteractions(continuations);
        verify(events).save(any());
    }
}
