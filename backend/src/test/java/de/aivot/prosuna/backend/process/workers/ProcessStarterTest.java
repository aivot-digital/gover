package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.process.ProcessTaskExecutionTestSupport;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessNodeRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeDefinitionService;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.process.services.ProcessTaskExecutionService;
import de.aivot.prosuna.backend.process.services.ProcessWorkRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProcessStarterTest {
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessRepository processes = mock(ProcessRepository.class);
    private final ProcessNodeRepository nodes = mock(ProcessNodeRepository.class);
    private final ProcessNodeDefinitionService definitions = mock(ProcessNodeDefinitionService.class);
    private final ProcessNodeExecutionLoggerFactory loggers = mock(ProcessNodeExecutionLoggerFactory.class);
    private final ProcessNodeExecutionLogger logger = mock(ProcessNodeExecutionLogger.class);
    private final ProcessTaskExecutionService execution = ProcessTaskExecutionTestSupport.passthrough();
    private final ProcessWorkRequestService workRequests = mock(ProcessWorkRequestService.class);
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setId(7L).setProcessId(1).setInitialNodeId(11)
            .setStatus(ProcessInstanceStatus.Created);
    private final ProcessStarter starter = new ProcessStarter(instances, processes, nodes, definitions, loggers, execution,
            workRequests);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        when(instances.findAllByStatus(ProcessInstanceStatus.Created)).thenReturn(List.of(instance));
        when(instances.findById(7L)).thenReturn(Optional.of(instance));
        when(loggers.create(7L, null, null, null)).thenReturn(logger);
        var node = new ProcessNodeEntity().setId(11).setName("Antrag prüfen").setProcessNodeDefinitionKey("review")
                .setProcessNodeDefinitionVersion(1);
        when(nodes.findById(11)).thenReturn(Optional.of(node));
        when(processes.findById(1)).thenReturn(Optional.of(new ProcessEntity().setId(1).setInternalTitle("Hundesteuer")));
        ProcessNodeDefinition<?> definition = mock(ProcessNodeDefinition.class);
        when(definitions.getProcessNodeDefinition("review", 1)).thenReturn(Optional.of(definition));
    }

    @Test
    void startsTheInstanceAndRequestsItsFirstTaskUnderTheInstanceLock() throws Exception {
        starter.startProcesses();

        assertEquals(ProcessInstanceStatus.Running, instance.getStatus());
        var order = inOrder(execution, instances, workRequests, logger);
        order.verify(execution).inInstance(eq(7L), any());
        order.verify(instances).save(instance);
        order.verify(workRequests).enqueueStart(7L);
        order.verify(logger).logf(ProcessNodeExecutionLogLevel.Info, true, true, "Neuer Vorgang „Hundesteuer“ gestartet",
                "Ein neuer Vorgang für den Prozess %s wurde gestartet. Die erste Aufgabe wird für das Prozesselement %s gestartet.",
                "„Hundesteuer“", "„Antrag prüfen“");
    }

    @Test
    void skipsAnInstanceThatWasStartedWhileWaitingForTheLock() {
        when(instances.findById(7L)).thenReturn(Optional.of(new ProcessInstanceEntity().setId(7L)
                .setStatus(ProcessInstanceStatus.Running)));

        starter.startProcesses();

        verify(instances, never()).save(any());
        verifyNoInteractions(workRequests, loggers);
    }

    @Test
    void failsAnInstanceWhoseInitialNodeIsMissingWithoutRequestingWork() {
        when(nodes.findById(11)).thenReturn(Optional.empty());

        starter.startProcesses();

        assertEquals(ProcessInstanceStatus.Failed, instance.getStatus());
        verify(instances).save(instance);
        verify(logger).logException(any(RuntimeException.class));
        verifyNoInteractions(workRequests);
    }

    @Test
    void continuesWithOtherInstancesWhenOneCannotBeLocked() throws Exception {
        var other = new ProcessInstanceEntity().setId(8L).setProcessId(1).setInitialNodeId(11)
                .setStatus(ProcessInstanceStatus.Created);
        when(instances.findAllByStatus(ProcessInstanceStatus.Created)).thenReturn(List.of(other, instance));
        doThrow(new IllegalStateException("lock timeout")).when(execution).inInstance(eq(8L), any());

        starter.startProcesses();

        assertEquals(ProcessInstanceStatus.Created, other.getStatus());
        verify(workRequests).enqueueStart(7L);
    }
}
