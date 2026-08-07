package de.aivot.gover.backend.payment.services;

import de.aivot.gover.backend.enums.XBezahldienstStatus;
import de.aivot.gover.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.repositories.ProcessInstanceTaskRepository;
import de.aivot.gover.backend.process.workers.ProcessWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentTransactionTaskResumeListenerTest {
    private ProcessInstanceTaskRepository processInstanceTaskRepository;
    private RabbitTemplate rabbitTemplate;
    private PaymentTransactionTaskResumeListener listener;

    @BeforeEach
    void setUp() {
        processInstanceTaskRepository = mock(ProcessInstanceTaskRepository.class);
        rabbitTemplate = mock(RabbitTemplate.class);
        listener = new PaymentTransactionTaskResumeListener(processInstanceTaskRepository, rabbitTemplate);
    }

    @Test
    void onChange_ShouldResumeTasksWhenTransactionWasPayed() throws Exception {
        var task = task();
        when(processInstanceTaskRepository.findAll(anySpecification())).thenReturn(List.of(task));

        listener.onChange(transaction(XBezahldienstStatus.PAYED));

        verify(rabbitTemplate).convertAndSend(
                eq(ProcessWorker.RESUME_WORK_ON_INSTANCE_QUEUE),
                eq(new ProcessWorker.ResumeWorkWorkerPayload(99L, 456L, 123))
        );
    }

    @Test
    void onChange_ShouldIgnoreTransactionsThatAreNotPayed() throws Exception {
        listener.onChange(transaction(XBezahldienstStatus.INITIAL));

        verifyNoInteractions(processInstanceTaskRepository, rabbitTemplate);
    }

    @Test
    void onDelete_ShouldResumeReferencedTasks() throws Exception {
        var task = task();
        when(processInstanceTaskRepository.findAll(anySpecification())).thenReturn(List.of(task));

        listener.onDelete(transaction(XBezahldienstStatus.CANCELED));

        verify(rabbitTemplate).convertAndSend(
                eq(ProcessWorker.RESUME_WORK_ON_INSTANCE_QUEUE),
                eq(new ProcessWorker.ResumeWorkWorkerPayload(99L, 456L, 123))
        );
    }

    private static PaymentTransactionEntity transaction(XBezahldienstStatus status) {
        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setStatus(status);

        return new PaymentTransactionEntity()
                .setKey("tx-1")
                .setPaymentInformation(paymentInformation);
    }

    private static ProcessInstanceTaskEntity task() {
        return new ProcessInstanceTaskEntity()
                .setId(456L)
                .setProcessInstanceId(99L)
                .setProcessNodeId(123);
    }

    private static Specification<ProcessInstanceTaskEntity> anySpecification() {
        return any();
    }
}
