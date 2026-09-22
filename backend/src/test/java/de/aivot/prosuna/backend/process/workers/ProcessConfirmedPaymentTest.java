package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.*;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionIO;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProcessConfirmedPaymentTest {
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessNodeExecutionResultHandler handler = mock(ProcessNodeExecutionResultHandler.class);
    private final ProcessDataService data = mock(ProcessDataService.class);
    private final ProcessNodeService nodes = mock(ProcessNodeService.class);
    private final ProcessWorker worker = new ProcessWorker(instances, null, null, tasks, handler, data, null, nodes, null);
    private final ProcessNodeExecutionLogger logger = mock(ProcessNodeExecutionLogger.class);
    private final ProcessNodeDefinition<Object> definition = mock(ProcessNodeDefinition.class);
    private final ProcessInstanceEntity instance = new ProcessInstanceEntity().setStatus(ProcessInstanceStatus.Running);
    private final ProcessInstanceTaskEntity task = new ProcessInstanceTaskEntity().setStatus(ProcessTaskStatus.AwaitingPayment)
            .setRuntimeData(Map.of("transaction", "existing-transaction"));
    private final ProcessNodeEntity node = new ProcessNodeEntity();
    private final Consumer<ProcessWorker.DoWorkWorkerPayload> outbox = mock(Consumer.class);

    @BeforeEach
    void setup() throws Exception {
        var executionData = mock(ProcessExecutionData.class);
        when(data.foldProcessInstanceData(instance, null, task)).thenReturn(executionData);
        when(nodes.deriveRuntimeConfiguration(node, definition, null, false, executionData))
                .thenReturn(new ProcessNodeService.ProcessConfigurationDetails<>(new Object(), DerivedRuntimeElementData.empty()));
    }

    @Test
    void reconcilesExistingPaymentWithoutInitializingAnotherTransaction() throws Exception {
        var completed = ProcessNodeExecutionResultTaskCompleted.of("paid");
        when(definition.resume(any())).thenReturn(completed);
        worker.resumeAfterConfirmedCommunication(logger, instance, task, node, definition, outbox);
        verify(definition).resume(argThat(context -> "existing-transaction".equals(context.getThisTask().getRuntimeData().get("transaction"))));
        verify(definition, never()).init(any());
        verify(handler).handleConfirmedCommunication(logger, null, definition, node, instance, task, null, completed, Map.of(), outbox);
    }

    @Test
    void failedPaymentIsRecordedWithoutUndoingDeliveryAcceptance() throws Exception {
        when(definition.resume(any())).thenThrow(new ProcessNodeExecutionExceptionIO("Payment failed"));
        worker.resumeAfterConfirmedCommunication(logger, instance, task, node, definition, outbox);
        assertEquals(ProcessTaskStatus.Failed, task.getStatus());
        assertEquals(ProcessInstanceStatus.Failed, instance.getStatus());
        verifyNoInteractions(handler, outbox);
        verify(definition, never()).init(any());
    }
}
