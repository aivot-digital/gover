package de.aivot.prosuna.backend.plugins.form.v1.services;

import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceHistoryEventRepository;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.prosuna.backend.process.services.ProcessNodeExecutionLoggerFactory;
import de.aivot.prosuna.backend.process.workers.ProcessWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FormPaymentTransactionChangeListenerTest {
    private final ProcessInstanceTaskRepository repository = mock(ProcessInstanceTaskRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final ProcessInstanceHistoryEventRepository events = mock(ProcessInstanceHistoryEventRepository.class);
    private final ProcessNodeExecutionLoggerFactory loggerFactory = mock(ProcessNodeExecutionLoggerFactory.class);
    private final FormPaymentTransactionChangeListener listener =
            new FormPaymentTransactionChangeListener(repository, rabbitTemplate, loggerFactory);

    @Test
    void onChange_QueuesResumeWorkForMatchingTasks() throws Exception {
        whenTasksAre(task(ProcessTaskStatus.AwaitingPayment));

        listener.onChange(transaction(XBezahldienstStatus.PAYED));

        verify(rabbitTemplate).convertAndSend(
                eq(ProcessWorker.RESUME_WORK_ON_INSTANCE_QUEUE),
                eq(new ProcessWorker.ResumeWorkWorkerPayload(99L, 456L, 123))
        );
        verifyNoInteractions(events);
    }

    @ParameterizedTest
    @EnumSource(value = ProcessTaskStatus.class, names = {"Failed", "Restarted"})
    void onChange_RecordsAPaymentThatCompletesAfterTheTaskEnded(ProcessTaskStatus status) throws Exception {
        whenTasksAre(task(status).setAssignedUserId("staff"));
        when(loggerFactory.create(99L, 456L, null, null)).thenReturn(new ProcessNodeExecutionLogger(99L, 456L, null, null, events));

        listener.onChange(transaction(XBezahldienstStatus.PAYED));

        var event = ArgumentCaptor.forClass(ProcessInstanceEventEntity.class);
        verify(events).save(event.capture());
        assertEquals("Zahlung für beendete Aufgabe eingegangen", event.getValue().getTitle());
        assertEquals(ProcessNodeExecutionLogLevel.Warn, event.getValue().getLevel());
        assertTrue(event.getValue().getHistoryRelevant());
        assertEquals("staff", event.getValue().getConcernedUserId());
        assertTrue(event.getValue().getMessage().contains("„tx-1“"));
        assertEquals(status.name(), event.getValue().getDetails().get("taskStatus"));
        verify(rabbitTemplate).convertAndSend(eq(ProcessWorker.RESUME_WORK_ON_INSTANCE_QUEUE), any(Object.class));
    }

    @Test
    void onChange_DoesNotRecordPaymentsThatDidNotComplete() throws Exception {
        whenTasksAre(task(ProcessTaskStatus.Restarted));

        listener.onChange(transaction(XBezahldienstStatus.CANCELED));

        verifyNoInteractions(loggerFactory, events);
    }

    @Test
    void onDelete_OnlyQueuesResumeWork() throws Exception {
        whenTasksAre(task(ProcessTaskStatus.Restarted));

        listener.onDelete(transaction(XBezahldienstStatus.PAYED));

        verify(rabbitTemplate).convertAndSend(eq(ProcessWorker.RESUME_WORK_ON_INSTANCE_QUEUE), any(Object.class));
        verifyNoInteractions(loggerFactory, events);
    }

    @SuppressWarnings("unchecked")
    private void whenTasksAre(ProcessInstanceTaskEntity... tasks) {
        when(repository.findAll(any(Specification.class))).thenReturn(List.of(tasks));
    }

    private static ProcessInstanceTaskEntity task(ProcessTaskStatus status) {
        return new ProcessInstanceTaskEntity()
                .setId(456L)
                .setProcessInstanceId(99L)
                .setProcessNodeId(123)
                .setStatus(status);
    }

    private static PaymentTransactionEntity transaction(XBezahldienstStatus status) {
        var information = new XBezahldienstePaymentInformation();
        information.setStatus(status);
        return new PaymentTransactionEntity().setKey("tx-1").setPaymentInformation(information);
    }
}
