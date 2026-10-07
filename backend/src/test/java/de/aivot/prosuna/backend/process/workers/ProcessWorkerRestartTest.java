package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceCompleted;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.services.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProcessWorkerRestartTest {
    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = {"Failed", "Restarted"})
    void persistsTheImmediateAttemptBeforeExecutingAndKeepsTheFlowPredecessor(ProcessTaskStatus oldStatus) throws Exception {
        var fixture = new Fixture();
        fixture.existing.put(100L, new ProcessInstanceTaskEntity().setId(100L).setProcessInstanceId(42L)
                .setProcessNodeId(11).setStatus(oldStatus).setRestartForTaskId(99L));

        fixture.worker.doWorkOnNextNode(new ProcessWorker.DoWorkWorkerPayload(42L, 5L, 10, "approved", 11, 100L));

        assertEquals(1, fixture.saved.size());
        var task = fixture.saved.getFirst();
        assertEquals(100L, task.getRestartForTaskId());
        assertEquals(5L, task.getPreviousProcessInstanceTaskId());
        assertEquals(10, task.getPreviousProcessNodeId());
        assertEquals("approved", task.getPreviousProcessNodePortKey());
        var order = inOrder(fixture.tasks, fixture.definition);
        order.verify(fixture.tasks).save(argThat(saved -> saved.getRestartForTaskId().equals(100L)));
        order.verify(fixture.definition).init(any());

        task.setStatus(ProcessTaskStatus.Failed);
        fixture.worker.doWorkOnNextNode(new ProcessWorker.DoWorkWorkerPayload(42L, 5L, 10, "approved", 11, task.getId()));
        assertEquals(task.getId(), fixture.saved.getLast().getRestartForTaskId());
        assertEquals(100L, task.getRestartForTaskId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"initial", "next", "loop"})
    void ordinaryExecutionDoesNotInventARestart(String kind) throws Exception {
        var fixture = new Fixture();
        Long previousTask = kind.equals("initial") ? null : 5L;
        Integer previousNode = kind.equals("initial") ? null : kind.equals("loop") ? 11 : 10;
        fixture.worker.doWorkOnNextNode(new ProcessWorker.DoWorkWorkerPayload(42L, previousTask, previousNode, "next", 11, null));
        assertEquals(1, fixture.saved.size());
        assertNull(fixture.saved.getFirst().getRestartForTaskId());
        verify(fixture.definition).init(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "instance", "node"})
    void rejectsInvalidRestartReferencesBeforeCreatingOrExecutingATask(String invalid) throws Exception {
        var fixture = new Fixture();
        if (!invalid.equals("missing")) {
            fixture.existing.put(100L, new ProcessInstanceTaskEntity().setId(100L)
                    .setProcessInstanceId(invalid.equals("instance") ? 99L : 42L)
                    .setProcessNodeId(invalid.equals("node") ? 99 : 11));
        }
        fixture.worker.doWorkOnNextNode(new ProcessWorker.DoWorkWorkerPayload(42L, null, null, null, 11, 100L));
        assertTrue(fixture.saved.isEmpty());
        verify(fixture.definition, never()).init(any());
        assertEquals(ProcessInstanceStatus.Failed, fixture.instance.getStatus());
    }

    @Test
    void resumePreservesTheExistingRestartReference() throws Exception {
        var fixture = new Fixture();
        var task = new ProcessInstanceTaskEntity().setId(100L).setProcessInstanceId(42L)
                .setProcessNodeId(11).setRestartForTaskId(99L);
        fixture.existing.put(100L, task);
        fixture.worker.resumeWorkOnCurrentNode(new ProcessWorker.ResumeWorkWorkerPayload(42L, 100L, 11));
        assertEquals(99L, task.getRestartForTaskId());
        assertTrue(fixture.saved.isEmpty());
        verify(fixture.definition).resume(any());
    }

    @Test
    void deserializesQueuedPayloadFromBeforeTheRestartFieldWasAdded() throws Exception {
        // Serialized with the original five-component DoWorkWorkerPayload record.
        var bytes = Base64.getDecoder().decode(
                "rO0ABXNyAEpkZS5haXZvdC5wcm9zdW5hLmJhY2tlbmQucHJvY2Vzcy53b3JrZXJzLlByb2Nlc3NXb3JrZXIkRG9Xb3JrV29ya2VyUGF5bG9hZAAAAAAAAAAAAgAFTAAKbmV4dE5vZGVJZHQAE0xqYXZhL2xhbmcvSW50ZWdlcjtMAA5wcmV2aW91c05vZGVJZHEAfgABTAATcHJldmlvdXNOb2RlUG9ydEtleXQAEkxqYXZhL2xhbmcvU3RyaW5nO0wADnByZXZpb3VzVGFza0lkdAAQTGphdmEvbGFuZy9Mb25nO0wAEXByb2Nlc3NJbnN0YW5jZUlkcQB+AAN4cHNyABFqYXZhLmxhbmcuSW50ZWdlchLioKT3gYc4AgABSQAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAALc3EAfgAFAAAACnQACGFwcHJvdmVkc3IADmphdmEubGFuZy5Mb25nO4vkkMyPI98CAAFKAAV2YWx1ZXhxAH4ABgAAAAAAAAAHc3EAfgAKAAAAAAAAACo=");
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            assertEquals(new ProcessWorker.DoWorkWorkerPayload(42L, 7L, 10, "approved", 11, null), input.readObject());
        }
        var payload = new ProcessWorker.DoWorkWorkerPayload(42L, 7L, 10, "approved", 11, 100L);
        var outputBytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(outputBytes)) {
            output.writeObject(payload);
        }
        try (var input = new ObjectInputStream(new ByteArrayInputStream(outputBytes.toByteArray()))) {
            assertEquals(payload, input.readObject());
        }
    }

    private static class Fixture {
        final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(42L).setProcessId(7).setStatus(ProcessInstanceStatus.Running);
        final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
        final HashMap<Long, ProcessInstanceTaskEntity> existing = new HashMap<>();
        final ArrayList<ProcessInstanceTaskEntity> saved = new ArrayList<>();
        final ProcessNodeDefinition<AuthoredElementValues> definition;
        final ProcessWorker worker;

        @SuppressWarnings("unchecked")
        Fixture() throws Exception {
            definition = mock(ProcessNodeDefinition.class);
            var node = new ProcessNodeEntity().setId(11).setProcessVersion(1).setName("Prüfung");
            var instances = mock(ProcessInstanceRepository.class);
            when(instances.findById(42L)).thenReturn(Optional.of(instance));
            var nodes = mock(ProcessNodeRepository.class);
            when(nodes.findById(11)).thenReturn(Optional.of(node));
            var definitions = mock(ProcessNodeDefinitionService.class);
            when(definitions.getProcessNodeDefinition(node)).thenReturn(Optional.of(definition));
            when(tasks.findById(anyLong())).thenAnswer(call -> Optional.ofNullable(existing.get(call.getArgument(0))));
            when(tasks.save(any())).thenAnswer(call -> {
                ProcessInstanceTaskEntity task = call.getArgument(0);
                if (task.getId() == null) task.setId(200L + saved.size());
                saved.add(task);
                existing.put(task.getId(), task);
                return task;
            });
            var data = new ProcessExecutionData();
            var dataService = mock(ProcessDataService.class);
            when(dataService.foldProcessInstanceData(any(), any(), any())).thenReturn(data);
            var nodeService = mock(ProcessNodeService.class);
            when(nodeService.deriveRuntimeConfiguration(node, definition, null, false, data))
                    .thenReturn(new ProcessNodeService.ProcessConfigurationDetails<>(new AuthoredElementValues(), new DerivedRuntimeElementData()));
            when(definition.init(any())).thenReturn(new ProcessNodeExecutionResultInstanceCompleted());
            when(definition.resume(any())).thenReturn(new ProcessNodeExecutionResultInstanceCompleted());
            var logger = mock(ProcessNodeExecutionLogger.class);
            when(logger.withTaskId(any())).thenReturn(logger);
            var loggers = mock(ProcessNodeExecutionLoggerFactory.class);
            when(loggers.create(any(), any(), any(), any())).thenReturn(logger);
            worker = new ProcessWorker(instances, nodes, definitions, tasks, mock(ProcessNodeExecutionResultHandler.class),
                    dataService, loggers, nodeService);
        }
    }
}
